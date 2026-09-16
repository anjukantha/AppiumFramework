package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import enums.Platform;
import enums.ScreenshotMode;

/**
 * Loads runtimeConfig.jsonc (server url, active platform, device/app details
 * per platform, screenshot settings,
 * report identity, retry count, driver capabilities) from the project root.
 * Active platform is resolved with precedence: "-Dplatform" system property,
 * then "PLATFORM" env var,
 * then runtimeConfig.jsonc's "platform" key, then default "android".
 */
public class ConfigManager {

    /**
     * Upper bound enforced on retry.count regardless of what's configured, to avoid
     * excessive re-runs.
     */
    private static final int MAX_RETRY_COUNT = 2;

    private static final ObjectMapper MAPPER = new ObjectMapper(
            JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                    .build());

    // Holds the config for the currently running test on this thread, so non-test
    // classes (listeners, Log) can read it.
    private static final ThreadLocal<ConfigManager> ACTIVE = new ThreadLocal<>();

    private final Map<String, String> properties = new HashMap<>();
    private final Platform platform;

    public ConfigManager() {
        loadRuntimeConfig("runtimeConfig.jsonc");
        this.platform = resolvePlatform();
        validateAppPath();
        ACTIVE.set(this);
    }

    private void validateAppPath() {
        String key = platformKey("appPath");
        String appPath = properties.get(key);
        if (appPath == null || appPath.isBlank()) {
            throw new IllegalStateException("Missing \"" + key + "\" in runtimeConfig.jsonc for platform '"
                    + platform.name().toLowerCase() + "'");
        }
        File appFile = new File(appPath);
        if (!appFile.exists()) {
            throw new IllegalStateException("Configured \"" + key + "\" does not exist: " + appFile.getAbsolutePath());
        }
    }

    /**
     * Resolves the active platform: -Dplatform system property > PLATFORM env var >
     * runtimeConfig.jsonc "platform" > "android".
     */
    private Platform resolvePlatform() {
        String value = System.getProperty("platform");
        if (value == null || value.isBlank()) {
            value = System.getenv("PLATFORM");
        }
        if (value == null || value.isBlank()) {
            value = properties.get("platform");
        }
        if (value == null || value.isBlank()) {
            value = "android";
        }
        return Platform.valueOf(value.trim().toUpperCase());
    }

    public static ConfigManager getActive() {
        return ACTIVE.get();
    }

    /**
     * Clears the thread-local config reference; call from test teardown to avoid
     * leaking across threads.
     */
    public static void clearActive() {
        ACTIVE.remove();
    }

    /**
     * Loads a jsonc file from the project root (rather than the classpath) so it's
     * easy to find and edit.
     */
    private void loadRuntimeConfig(String fileName) {
        File file = new File(fileName);
        try (InputStream in = new FileInputStream(file)) {
            flatten("", MAPPER.readTree(in));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Flattens nested JSON objects into dot-separated keys, e.g.
     * {"screenshot":{"mode":"all"}} -> "screenshot.mode".
     */
    private void flatten(String prefix, JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue().isObject()) {
                flatten(key, entry.getValue());
            } else {
                properties.put(key, entry.getValue().asText());
            }
        }
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getAppiumServerUrl() {
        return properties.get("appium.server.url");
    }

    public String getPlatformVersion() {
        return properties.get(platformKey("platformVersion"));
    }

    public String getAppPath() {
        return properties.get(platformKey("appPath"));
    }

    /**
     * Comma-separated device UDIDs to pin the appium-device-farm plugin's df:udids
     * capability to, so sessions only
     * run on those specific devices instead of whichever one the plugin
     * auto-allocates. Resolved with the same
     * precedence as the active platform: -Dudid=&lt;value&gt; system property, then
     * UDID env var, then
     * runtimeConfig.jsonc's android.udids/ios.udids key; returns null if none of
     * those are set.
     */
    public String getDeviceUdids() {
        String value = System.getProperty("udid");
        if (value == null || value.isBlank()) {
            value = System.getenv("UDID");
        }
        if (value == null || value.isBlank()) {
            value = properties.get(platformKey("udids"));
        }
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * Builds the runtimeConfig.jsonc key for the given field under the active
     * platform's block, e.g. "android.appPath".
     */
    private String platformKey(String field) {
        return platform.name().toLowerCase() + "." + field;
    }

    /**
     * Policy for the single screenshot taken at test end based on its final result:
     * "failure" (default), "pass", "all" or "none".
     */
    public ScreenshotMode getScreenshotMode() {
        String value = properties.getOrDefault("screenshot.mode", "failure");
        try {
            return ScreenshotMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ScreenshotMode.FAILURE;
        }
    }

    /**
     * Whether every logged step (Log.info/pass/warn/error) should ALSO attach its
     * own screenshot, on top of getScreenshotMode()'s end-of-test one.
     */
    public boolean isScreenshotOnStep() {
        return Boolean.parseBoolean(properties.getOrDefault("screenshot.onStep", "false"));
    }

    public String getProductName() {
        return properties.getOrDefault("report.productName", "");
    }

    public String getTeamName() {
        return properties.getOrDefault("report.teamName", "");
    }

    public String getRunType() {
        return properties.getOrDefault("report.runType", "");
    }

    /**
     * Number of times a failed @Test is retried; 0 (default) disables retries;
     * clamped to [0, MAX_RETRY_COUNT].
     */
    public int getRetryCount() {
        int value;
        try {
            value = Integer.parseInt(properties.getOrDefault("retry.count", "0"));
        } catch (NumberFormatException e) {
            value = 0;
        }
        return Math.max(0, Math.min(value, MAX_RETRY_COUNT));
    }

    /**
     * Whether to keep app data/state between sessions instead of resetting it (both
     * platforms); default true.
     */
    public boolean isNoReset() {
        return Boolean.parseBoolean(properties.getOrDefault("driver.noReset", "true"));
    }

    /**
     * Android (UiAutomator2) only: whether to suppress errors from apps using
     * restricted/hidden APIs; default true.
     */
    public boolean isIgnoreHiddenApiPolicyError() {
        return Boolean.parseBoolean(properties.getOrDefault("driver.ignoreHiddenApiPolicyError", "true"));
    }

    /**
     * Whether to uninstall/reinstall the app before the session for a clean state
     * (both platforms); default false.
     */
    public boolean isFullReset() {
        return Boolean.parseBoolean(properties.getOrDefault("driver.fullReset", "false"));
    }

    /**
     * Android (UiAutomator2) only: whether to auto-grant all requested app
     * permissions at install time; default true.
     */
    public boolean isAutoGrantPermissions() {
        return Boolean.parseBoolean(properties.getOrDefault("driver.autoGrantPermissions", "true"));
    }

    /**
     * iOS (XCUITest) only: whether to auto-accept iOS system alerts as they appear;
     * default true.
     */
    public boolean isAutoAcceptAlerts() {
        return Boolean.parseBoolean(properties.getOrDefault("driver.autoAcceptAlerts", "true"));
    }

    /**
     * Gets the new command timeout in seconds.
     *
     * @return the new command timeout in seconds
     */
    public int getNewCommandTimeoutSeconds() {
        try {
            return Integer.parseInt(properties.getOrDefault("driver.newCommandTimeout", "120"));
        } catch (NumberFormatException e) {
            return 120;
        }
    }

    /**
     * Gets the wait timeout in seconds.
     *
     * @return the wait timeout in seconds
     */
    public int getWaitTimeoutSeconds() {
        try {
            return Integer.parseInt(properties.getOrDefault("wait.timeoutSeconds", "15"));
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    /**
     * Gets the appId (package name for Android, bundle ID for iOS) of the app under
     * test, used for uninstalling/reinstalling the app and for launching
     * it/reopening it after a session.
     *
     * @return the appId of the app under test
     */
    public String getAppId() {
        String appId = properties.get(platformKey("appId"));
        return (appId == null || appId.isBlank()) ? null : appId.trim();
    }
}