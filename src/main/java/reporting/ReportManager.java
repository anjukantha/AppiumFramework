package reporting;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reporting.model.SuiteRecord;
import reporting.model.TestAttempt;
import reporting.model.TestRecord;

/**
 * Owns the in-memory report model for the whole run; ReportListener populates
 * it and renders it at suite end.
 */
public final class ReportManager {

    private static final String RUN_TIMESTAMP = LocalDateTime.now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    private static final String REPORT_DIR = "test-output" + File.separator + RUN_TIMESTAMP;

    private static final SuiteRecord SUITE = new SuiteRecord();

    // Keyed by "className#methodName" so retry attempts of the same test share one
    // TestRecord.
    private static final Map<String, TestRecord> TESTS_BY_KEY = new ConcurrentHashMap<>();

    private static final ThreadLocal<TestRecord> CURRENT_TEST = new ThreadLocal<>();
    private static final ThreadLocal<TestAttempt> CURRENT_ATTEMPT = new ThreadLocal<>();

    private ReportManager() {
    }

    public static String getReportDir() {
        return REPORT_DIR;
    }

    public static String getScreenshotDir() {
        return REPORT_DIR + File.separator + "screenshots";
    }

    public static SuiteRecord getSuite() {
        return SUITE;
    }

    public static TestAttempt startTest(String testKey, String testName, String platform) {
        TestRecord testRecord = TESTS_BY_KEY.computeIfAbsent(testKey, key -> {
            TestRecord created = new TestRecord(testName, platform);
            SUITE.addTest(created);
            return created;
        });
        TestAttempt attempt = testRecord.startAttempt();
        CURRENT_TEST.set(testRecord);
        CURRENT_ATTEMPT.set(attempt);
        return attempt;
    }

    public static TestAttempt getCurrentAttempt() {
        return CURRENT_ATTEMPT.get();
    }

    public static void endTest() {
        TestRecord testRecord = CURRENT_TEST.get();
        if (testRecord != null) {
            testRecord.finish();
        }
        CURRENT_TEST.remove();
        CURRENT_ATTEMPT.remove();
    }
}