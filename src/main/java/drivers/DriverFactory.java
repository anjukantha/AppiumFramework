package drivers;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path appPath = Paths.get(config.getAppPath()).toAbsolutePath();

        String udids = config.getDeviceUdids();

        if (config.getPlatform() == Platform.IOS) {
            XCUITestOptions options = new XCUITestOptions();
            options.setPlatformVersion(config.getPlatformVersion());
            options.setApp(appPath.toString());
            options.setNoReset(config.isNoReset());
            options.setFullReset(config.isFullReset());
            options.setAutoAcceptAlerts(config.isAutoAcceptAlerts());
            if (udids != null) {
                options.setCapability("df:udids", udids);
            }
            return new IOSDriver(serverUrl, options);
        }

        UiAutomator2Options options = new UiAutomator2Options();
        options.setIgnoreHiddenApiPolicyError(config.isIgnoreHiddenApiPolicyError());
        options.setNoReset(config.isNoReset());
        options.setFullReset(config.isFullReset());
        options.setAutoGrantPermissions(config.isAutoGrantPermissions());
        options.setApp(appPath.toString());
        if (udids != null) {
            options.setCapability("df:udids", udids);
        }
        return new AndroidDriver(serverUrl, options);
    }
}