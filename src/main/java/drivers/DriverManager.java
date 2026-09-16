package drivers;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;

/**
 * Holds the active driver per thread so parallel test execution doesn't share
 * sessions.
 */
public final class DriverManager {

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void setDriver(AppiumDriver driver) {
        DRIVER.set(driver);
    }

    public static AppiumDriver getDriver() {
        return DRIVER.get();
    }

    public static void quitDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }

    // Restarts the app under test by terminating it and then activating it again.
    public static void restartApp(String appId) {
        AppiumDriver driver = DRIVER.get();
        if (driver instanceof InteractsWithApps && appId != null) {
            InteractsWithApps app = (InteractsWithApps) driver;
            app.terminateApp(appId);
            app.activateApp(appId);
        }
    }
}