package listeners;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.SkipException;

import config.ConfigManager;
import drivers.DriverManager;
import enums.ScreenshotMode;
import enums.StepStatus;
import enums.TestStatus;
import io.appium.java_client.AppiumDriver;
import reporting.HtmlReportRenderer;
import reporting.ReportManager;
import reporting.model.StepLog;
import reporting.model.SuiteRecord;
import reporting.model.TestAttempt;
import utils.ScreenshotUtils;

/**
 * Populates the custom HTML report model (reporting.ReportManager) from TestNG
 * suite/test events.
 */
public class ReportListener implements ITestListener, ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        ConfigManager config = new ConfigManager();
        SuiteRecord suiteRecord = ReportManager.getSuite();
        suiteRecord.setProductName(config.getProductName());
        suiteRecord.setTeamName(config.getTeamName());
        suiteRecord.setRunType(config.getRunType());
        suiteRecord.setEnvironment(config.getPlatform().name());
        suiteRecord.setPlatformVersion(devicePlatformVersion(config));
        suiteRecord.setAppPath(config.getAppPath());
        suiteRecord.setDeviceUdids(config.getDeviceUdids() != null ? config.getDeviceUdids() : "auto-allocated");
        suiteRecord.setPlannedCount(suite.getAllMethods().size());
        suiteRecord.setStartTime(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    private String devicePlatformVersion(ConfigManager config) {
        String version = config.getPlatformVersion();
        return (version == null || version.isBlank()) ? "auto" : version;
    }

    @Override
    public void onFinish(ISuite suite) {
        SuiteRecord suiteRecord = ReportManager.getSuite();
        suiteRecord.setEndTime(ZonedDateTime.now(ZoneId.systemDefault()));
        HtmlReportRenderer.render(suiteRecord, ReportManager.getReportDir());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String key = result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
        ConfigManager config = ConfigManager.getActive();
        String platform = config != null ? config.getPlatform().name() : "UNKNOWN";
        ReportManager.startTest(key, result.getMethod().getMethodName(), platform);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        finishAttempt(result, TestStatus.PASS, null, ScreenshotMode.PASS);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String message = result.getThrowable() != null ? result.getThrowable().getMessage() : "Test failed";
        finishAttempt(result, TestStatus.FAIL, message, ScreenshotMode.FAILURE);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (isRetriedFailure(result)) {
            // TestNG reports an attempt that will be retried via onTestSkipped (not
            // onTestFailure); treat it as a
            // real failure so it's shown/screenshotted like one, instead of as a genuine
            // skip.
            onTestFailure(result);
            return;
        }
        String message = result.getThrowable() != null ? result.getThrowable().getMessage() : "Test skipped";
        TestAttempt attempt = ReportManager.getCurrentAttempt();
        if (attempt != null) {
            attempt.finish(TestStatus.SKIP, message);
        }
        ReportManager.endTest();
    }

    /**
     * Distinguishes a genuine skip (SkipException, e.g. a failed dependency) from a
     * retried failed attempt.
     */
    private boolean isRetriedFailure(ITestResult result) {
        return result.getThrowable() != null && !(result.getThrowable() instanceof SkipException);
    }

    private void finishAttempt(ITestResult result, TestStatus status, String errorMessage,
            ScreenshotMode requiredMode) {
        TestAttempt attempt = ReportManager.getCurrentAttempt();
        if (attempt != null) {
            attempt.finish(status, errorMessage);
            if (status == TestStatus.FAIL) {
                attempt.addStep(new StepLog(StepStatus.FAIL, errorDetail(result), null));
                attachPageSource(attempt);
            }
            if (shouldCapture(requiredMode)) {
                attachScreenshot(attempt, result, status);
            }
        }
        ReportManager.endTest();
    }

    /**
     * Full stack trace of the test failure, so it shows up as a step alongside the
     * logged steps.
     */
    private String errorDetail(ITestResult result) {
        Throwable throwable = result.getThrowable();
        if (throwable == null) {
            return "Test failed";
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void attachPageSource(TestAttempt attempt) {
        AppiumDriver driver = DriverManager.getDriver();
        if (driver == null) {
            return;
        }
        try {
            String pageSource = driver.getPageSource();
            if (pageSource != null && !pageSource.isBlank()) {
                attempt.addStep(new StepLog(StepStatus.FAIL, "Page Source at failure:\n" + pageSource, null));
            }
        } catch (RuntimeException e) {
            attempt.addStep(new StepLog(StepStatus.WARN, "Could not capture page source:\n " + e.getMessage(), null));
        }
    }

    private void attachScreenshot(TestAttempt attempt, ITestResult result, TestStatus status) {
        String path = ScreenshotUtils.capture(
                DriverManager.getDriver(), ReportManager.getScreenshotDir(), result.getMethod().getMethodName());
        if (path != null) {
            StepStatus stepStatus = status == TestStatus.PASS ? StepStatus.PASS : StepStatus.FAIL;
            attempt.addStep(new StepLog(stepStatus, "Screenshot at test end", path));
        }
    }

    private boolean shouldCapture(ScreenshotMode required) {
        ConfigManager config = ConfigManager.getActive();
        if (config == null) {
            return false;
        }
        ScreenshotMode mode = config.getScreenshotMode();
        return mode == ScreenshotMode.ALL || mode == required;
    }
}