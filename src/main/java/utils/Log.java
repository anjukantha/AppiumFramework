package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import config.ConfigManager;
import drivers.DriverManager;
import enums.StepStatus;
import reporting.ReportManager;
import reporting.model.StepLog;
import reporting.model.TestAttempt;

/**
 * Single entry point for step logging: writes to the log file/console and, if a
 * test is running, the HTML report.
 */
public final class Log {

    private static final Logger LOGGER = LogManager.getLogger(Log.class);

    private Log() {
    }

    public static void info(String message) {
        LOGGER.info(message);
        logToReport(StepStatus.INFO, message);
    }

    public static void pass(String message) {
        LOGGER.info(message);
        logToReport(StepStatus.PASS, message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
        logToReport(StepStatus.WARN, message);
    }

    public static void error(String message) {
        LOGGER.error(message);
        logToReport(StepStatus.FAIL, message);
    }

    private static void logToReport(StepStatus status, String message) {
        TestAttempt attempt = ReportManager.getCurrentAttempt();
        if (attempt == null) {
            return;
        }
        String screenshotPath = isScreenshotOnStepEnabled() ? captureStepScreenshot(status) : null;
        attempt.addStep(new StepLog(status, message, screenshotPath));
    }

    private static boolean isScreenshotOnStepEnabled() {
        ConfigManager config = ConfigManager.getActive();
        return config != null && config.isScreenshotOnStep() && DriverManager.getDriver() != null;
    }

    private static String captureStepScreenshot(StepStatus status) {
        return ScreenshotUtils.capture(
                DriverManager.getDriver(), ReportManager.getScreenshotDir(), status.name().toLowerCase());
    }
}