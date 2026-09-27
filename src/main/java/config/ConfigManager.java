package config;
 
 import com.fasterxml.jackson.core.JsonFactory;
 import com.fasterxml.jackson.core.json.JsonReadFeature;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import enums.Platform;
 import enums.ScreenshotMode;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.MalformedURLException;
 import java.net.URL;
 
 /** Loads and validates the typed runtimeConfig.jsonc model. */
 public class ConfigManager {
 
     private static final int MAX_RETRY_COUNT = 2;
     private static final String CONFIG_FILE = "runtimeConfig.jsonc";
 
     private static final ObjectMapper MAPPER = new ObjectMapper(
             JsonFactory.builder()
                     .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                     .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                     .build());
 
     private static final ThreadLocal<ConfigManager> ACTIVE = new ThreadLocal<>();
 
     private final RuntimeConfig runtimeConfig;
     private final Platform platform;
 
     public ConfigManager() {
         runtimeConfig = loadRuntimeConfig(CONFIG_FILE);
         platform = resolvePlatform(execution().platform);
         validateRequiredConfiguration();
         ACTIVE.set(this);
     }
 
     private RuntimeConfig loadRuntimeConfig(String fileName) {
         File file = new File(fileName);
         try (InputStream input = new FileInputStream(file)) {
             RuntimeConfig loadedConfig = MAPPER.readValue(input, RuntimeConfig.class);
             if (loadedConfig == null) {
                 throw new IllegalStateException("Configuration file is empty: " + file.getAbsolutePath());
             }
             return loadedConfig;
         } catch (IOException e) {
             throw new IllegalStateException("Failed to load configuration file: " + file.getAbsolutePath(), e);
         }
     }
 
     private Platform resolvePlatform(String configuredPlatform) {
         String value = System.getProperty("platform");
         if (value == null || value.isBlank()) {
             value = System.getenv("PLATFORM");
         }
         if (value == null || value.isBlank()) {
             value = configuredPlatform;
         }
         if (value == null || value.isBlank()) {
             value = "android";
         }
         try {
             return Platform.valueOf(value.trim().toUpperCase());
         } catch (IllegalArgumentException e) {
             throw new IllegalStateException("Invalid configuration 'execution.platform': '" + value
                     + "'. Expected 'android' or 'ios'.", e);
         }
     }
 
     private void validateRequiredConfiguration() {
         AppiumConfig appium = requiredAppiumConfig();
         String serverUrl = requiredValue(appium.serverUrl, "appium.serverUrl");
         try {
             new URL(serverUrl);
         } catch (MalformedURLException e) {
             throw new IllegalStateException("Invalid configuration 'appium.serverUrl': '" + serverUrl
                     + "'. Expected a URL such as http://127.0.0.1:4723.", e);
         }
 
         AppConfig appConfig = activeAppConfig();
         String appIdentifier = platform == Platform.ANDROID ? appConfig.appPackage : appConfig.bundleId;
         if ((appConfig.app == null || appConfig.app.isBlank())
             && (appIdentifier == null || appIdentifier.isBlank())) {
             throw new IllegalStateException("Missing application launch target in " + CONFIG_FILE
                 + ". Configure either '" + platformKey("app") + "' or '"
                 + platformKey(platform == Platform.ANDROID ? "appPackage" : "bundleId") + "'.");
         }
     }
 
     private String requiredValue(String value, String key) {
         if (value == null || value.isBlank()) {
             throw new IllegalStateException("Missing required configuration '" + key
                     + "' in " + CONFIG_FILE + ".");
         }
         return value.trim();
     }
 
     private AppiumConfig requiredAppiumConfig() {
         if (runtimeConfig.appium == null) {
             throw new IllegalStateException("Missing required configuration section 'appium' in "
                     + CONFIG_FILE + ".");
         }
         return runtimeConfig.appium;
     }
 
     private AppConfig activeAppConfig() {
         AppiumConfig appium = requiredAppiumConfig();
         AppConfig appConfig = platform == Platform.ANDROID ? appium.android : appium.ios;
         if (appConfig == null) {
             throw new IllegalStateException("Missing required configuration section '" + platformKey("")
                     + "' in " + CONFIG_FILE + ".");
         }
         return appConfig;
     }
 
     private ExecutionConfig execution() {
         return runtimeConfig.execution == null ? new ExecutionConfig() : runtimeConfig.execution;
     }
 
     private CommonCapabilities commonCapabilities() {
         CapabilitiesConfig capabilities = runtimeConfig.capabilities;
         return capabilities == null || capabilities.common == null
                 ? new CommonCapabilities()
                 : capabilities.common;
     }
 
     private AndroidCapabilities androidCapabilities() {
         CapabilitiesConfig capabilities = runtimeConfig.capabilities;
         return capabilities == null || capabilities.android == null
                 ? new AndroidCapabilities()
                 : capabilities.android;
     }
 
     private IosCapabilities iosCapabilities() {
         CapabilitiesConfig capabilities = runtimeConfig.capabilities;
         return capabilities == null || capabilities.ios == null
                 ? new IosCapabilities()
                 : capabilities.ios;
     }
 
     private ScreenshotConfig screenshot() {
         return runtimeConfig.screenshot == null ? new ScreenshotConfig() : runtimeConfig.screenshot;
     }
 
     private ReportConfig report() {
         return runtimeConfig.report == null ? new ReportConfig() : runtimeConfig.report;
     }
 
     private String platformKey(String field) {
         return "appium." + platform.name().toLowerCase() + (field.isEmpty() ? "" : "." + field);
     }
 
     private static boolean booleanValue(Boolean value, boolean defaultValue) {
         return value == null ? defaultValue : value;
     }
 
     private static int integerValue(Integer value, int defaultValue, String key) {
         int resolved = value == null ? defaultValue : value;
         if (resolved < 0) {
             throw new IllegalStateException("Invalid configuration '" + key + "': " + resolved
                     + ". Expected a non-negative integer.");
         }
         return resolved;
     }
 
     public static ConfigManager getActive() {
         return ACTIVE.get();
     }
 
     public static void clearActive() {
         ACTIVE.remove();
     }
 
     public Platform getPlatform() {
         return platform;
     }
 
     public String getAppiumServerUrl() {
         return requiredValue(requiredAppiumConfig().serverUrl, "appium.serverUrl");
     }
 
     public String getPlatformVersion() {
         String version = activeAppConfig().platformVersion;
         return version == null ? "" : version.trim();
     }
 
     public String getAppPath() {
         return valueOrEmpty(activeAppConfig().app);
     }
 
     public String getDeviceUdids() {
         String value = System.getProperty("udid");
         if (value == null || value.isBlank()) {
             value = System.getenv("UDID");
         }
         if (value == null || value.isBlank()) {
             value = activeAppConfig().udids;
         }
         return value == null || value.isBlank() ? null : value.trim();
     }
 
     public ScreenshotMode getScreenshotMode() {
         String value = screenshot().mode;
         if (value == null || value.isBlank()) {
             return ScreenshotMode.FAILURE;
         }
         try {
             return ScreenshotMode.valueOf(value.trim().toUpperCase());
         } catch (IllegalArgumentException e) {
             throw new IllegalStateException("Invalid configuration 'screenshot.mode': '" + value
                     + "'. Expected failure, pass, all, or none.", e);
         }
     }
 
     public boolean isScreenshotOnStep() {
         return booleanValue(screenshot().onStep, false);
     }
 
     public String getProductName() {
         return valueOrEmpty(report().productName);
     }
 
     public String getTeamName() {
         return valueOrEmpty(report().teamName);
     }
 
     public String getRunType() {
         return valueOrEmpty(report().runType);
     }
 
     public int getRetryCount() {
         int value = integerValue(execution().retryCount, 0, "execution.retryCount");
         return Math.min(value, MAX_RETRY_COUNT);
     }
 
     public boolean isNoReset() {
         return booleanValue(commonCapabilities().noReset, true);
     }
 
     public boolean isIgnoreHiddenApiPolicyError() {
         return booleanValue(androidCapabilities().ignoreHiddenApiPolicyError, true);
     }
 
     public boolean isFullReset() {
         return booleanValue(commonCapabilities().fullReset, false);
     }
 
     public boolean isAutoGrantPermissions() {
         return booleanValue(androidCapabilities().autoGrantPermissions, true);
     }
 
     public int getAdbExecTimeoutMillis() {
         return integerValue(androidCapabilities().adbExecTimeout, 120000,
                 "capabilities.android.adbExecTimeout");
     }
 
     public int getAndroidInstallTimeoutMillis() {
         return integerValue(androidCapabilities().androidInstallTimeout, 180000,
                 "capabilities.android.androidInstallTimeout");
     }
 
     public int getUiAutomator2ServerInstallTimeoutMillis() {
         return integerValue(androidCapabilities().uiautomator2ServerInstallTimeout, 120000,
                 "capabilities.android.uiautomator2ServerInstallTimeout");
     }
 
     public int getUiAutomator2ServerLaunchTimeoutMillis() {
         return integerValue(androidCapabilities().uiautomator2ServerLaunchTimeout, 120000,
                 "capabilities.android.uiautomator2ServerLaunchTimeout");
     }
 
     public int getAppWaitDurationMillis() {
         return integerValue(androidCapabilities().appWaitDuration, 120000,
                 "capabilities.android.appWaitDuration");
     }
 
     public boolean isAutoAcceptAlerts() {
         return booleanValue(iosCapabilities().autoAcceptAlerts, true);
     }
 
     public int getWdaLaunchTimeoutMillis() {
         return integerValue(iosCapabilities().wdaLaunchTimeout, 120000,
                 "capabilities.ios.wdaLaunchTimeout");
     }
 
     public int getWdaConnectionTimeoutMillis() {
         return integerValue(iosCapabilities().wdaConnectionTimeout, 120000,
                 "capabilities.ios.wdaConnectionTimeout");
     }
 
     public int getAppPushTimeoutMillis() {
         return integerValue(iosCapabilities().appPushTimeout, 180000,
                 "capabilities.ios.appPushTimeout");
     }
 
     public int getIosInstallPauseMillis() {
         return integerValue(iosCapabilities().iosInstallPause, 10000,
                 "capabilities.ios.iosInstallPause");
     }
 
     public int getWdaStartupRetries() {
         return integerValue(iosCapabilities().wdaStartupRetries, 3,
                 "capabilities.ios.wdaStartupRetries");
     }
 
     public int getWdaStartupRetryIntervalMillis() {
         return integerValue(iosCapabilities().wdaStartupRetryInterval, 10000,
                 "capabilities.ios.wdaStartupRetryInterval");
     }
 
     public int getNewCommandTimeoutSeconds() {
         return integerValue(commonCapabilities().newCommandTimeout, 120,
                 "capabilities.common.newCommandTimeout");
     }
 
     public int getWaitTimeoutSeconds() {
         return integerValue(execution().waitTimeoutSeconds, 15, "execution.waitTimeoutSeconds");
     }
 
     public String getAppId() {
         String value = platform == Platform.ANDROID ? activeAppConfig().appPackage : activeAppConfig().bundleId;
         return value == null || value.isBlank() ? null : value.trim();
     }
 
     private static String valueOrEmpty(String value) {
         return value == null ? "" : value.trim();
     }

     public static class RuntimeConfig {
         public ExecutionConfig execution = new ExecutionConfig();
         public AppiumConfig appium = new AppiumConfig();
         public CapabilitiesConfig capabilities = new CapabilitiesConfig();
         public ScreenshotConfig screenshot = new ScreenshotConfig();
         public ReportConfig report = new ReportConfig();
     }
 
     public static class ExecutionConfig {
         public String platform = "android";
         public Integer retryCount = 0;
         public Integer waitTimeoutSeconds = 15;
     }
 
     public static class AppiumConfig {
         public String serverUrl;
         public AppConfig android = new AppConfig();
         public AppConfig ios = new AppConfig();
     }
 
     public static class AppConfig {
         public String app;
         public String appPackage;
         public String bundleId;
         public String platformVersion = "";
         public String udids;
     }
 
     public static class CapabilitiesConfig {
         public CommonCapabilities common = new CommonCapabilities();
         public AndroidCapabilities android = new AndroidCapabilities();
         public IosCapabilities ios = new IosCapabilities();
     }
 
     public static class CommonCapabilities {
         public Boolean noReset = true;
         public Boolean fullReset = false;
         public Integer newCommandTimeout = 120;
     }
 
     public static class AndroidCapabilities {
         public Boolean ignoreHiddenApiPolicyError = true;
         public Boolean autoGrantPermissions = true;
         public Integer adbExecTimeout = 120000;
         public Integer androidInstallTimeout = 180000;
         public Integer uiautomator2ServerInstallTimeout = 120000;
         public Integer uiautomator2ServerLaunchTimeout = 120000;
         public Integer appWaitDuration = 120000;
     }
 
     public static class IosCapabilities {
         public Boolean autoAcceptAlerts = true;
         public Integer wdaLaunchTimeout = 120000;
         public Integer wdaConnectionTimeout = 120000;
         public Integer appPushTimeout = 180000;
         public Integer iosInstallPause = 10000;
         public Integer wdaStartupRetries = 3;
         public Integer wdaStartupRetryInterval = 10000;
     }
 
     public static class ScreenshotConfig {
         public String mode = "failure";
         public Boolean onStep = false;
     }
 
     public static class ReportConfig {
         public String productName = "";
         public String teamName = "";
         public String runType = "";
     }
 }