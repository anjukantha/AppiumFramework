package drivers;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;

import config.ConfigManager;
import enums.Platform;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

/**
 * Builds the right AppiumDriver/options for the active platform from
 * ConfigManager.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static AppiumDriver createDriver(ConfigManager config) throws MalformedURLException {
        URL serverUrl = new URL(config.getAppiumServerUrl());
        String appPath = config.getAppPath();
        String appId = config.getAppId();

        String udids = config.getDeviceUdids();
        Duration newCommandTimeout = Duration.ofSeconds(config.getNewCommandTimeoutSeconds());

        if (config.getPlatform() == Platform.IOS) {
            XCUITestOptions options = new XCUITestOptions();
            setPlatformVersion(options, config.getPlatformVersion());
            if (!appPath.isBlank()) {
                options.setApp(toAbsolutePath(appPath));
            } else if (appId != null) {
                options.setBundleId(appId);
            }
            options.setNoReset(config.isNoReset());
            options.setFullReset(config.isFullReset());
            options.setAutoAcceptAlerts(config.isAutoAcceptAlerts());
            options.setWdaLaunchTimeout(Duration.ofMillis(config.getWdaLaunchTimeoutMillis()));
            options.setWdaConnectionTimeout(Duration.ofMillis(config.getWdaConnectionTimeoutMillis()));
            options.setAppPushTimeout(Duration.ofMillis(config.getAppPushTimeoutMillis()));
            options.setCapability("iosInstallPause", config.getIosInstallPauseMillis());
            options.setWdaStartupRetries(config.getWdaStartupRetries());
            options.setWdaStartupRetryInterval(Duration.ofMillis(config.getWdaStartupRetryIntervalMillis()));
            options.setNewCommandTimeout(newCommandTimeout);
            if (udids != null) {
                options.setCapability("df:udids", udids);
            }
            return new IOSDriver(serverUrl, options);
        }

        UiAutomator2Options options = new UiAutomator2Options();
        setPlatformVersion(options, config.getPlatformVersion());
        if (!appPath.isBlank()) {
            options.setApp(toAbsolutePath(appPath));
        } else if (appId != null) {
            options.setAppPackage(appId);
        }
        options.setAdbExecTimeout(Duration.ofMillis(config.getAdbExecTimeoutMillis()));
        options.setAndroidInstallTimeout(Duration.ofMillis(config.getAndroidInstallTimeoutMillis()));
        options.setAppWaitDuration(Duration.ofMillis(config.getAppWaitDurationMillis()));
        options.setUiautomator2ServerLaunchTimeout(
            Duration.ofMillis(config.getUiAutomator2ServerLaunchTimeoutMillis()));
        options.setUiautomator2ServerInstallTimeout(
            Duration.ofMillis(config.getUiAutomator2ServerInstallTimeoutMillis()));
        options.setIgnoreHiddenApiPolicyError(config.isIgnoreHiddenApiPolicyError());
        options.setNoReset(config.isNoReset());
        options.setFullReset(config.isFullReset());
        options.setAutoGrantPermissions(config.isAutoGrantPermissions());
        options.setNewCommandTimeout(newCommandTimeout);
        if (udids != null) {
            options.setCapability("df:udids", udids);
        }
        return new AndroidDriver(serverUrl, options);
    }

    private static String toAbsolutePath(String appPath) {
        return Paths.get(appPath).toAbsolutePath().toString();
    }

    private static void setPlatformVersion(
        io.appium.java_client.remote.options.BaseOptions<?> options, String platformVersion) {
        if (!platformVersion.isBlank()) {
            options.setPlatformVersion(platformVersion);
        }
    }
}