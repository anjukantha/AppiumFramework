package drivers;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;

import java.util.Objects;

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
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }

    // Restarts the app under test by terminating it and then activating it again.
    public static void restartApp(String appId) {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("Cannot restart the app because no driver is active.");
        }
        if (!(driver instanceof InteractsWithApps)) {
            throw new IllegalStateException("The active driver does not support app lifecycle operations.");
        }
        String requiredAppId = Objects.requireNonNull(appId,
                "An app package or bundle ID is required to restart the app between retries.");
        InteractsWithApps app = (InteractsWithApps) driver;
        app.terminateApp(requiredAppId);
        app.activateApp(requiredAppId);
    }
}