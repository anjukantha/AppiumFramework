package reporting.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import enums.TestStatus;

/**
 * A single @Test method's run, including its initial attempt and any retries.
 */
public class TestRecord {

    private final String testName;
    private final String platform;
    private final ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault());
    private ZonedDateTime endTime;
    private final List<TestAttempt> attempts = new CopyOnWriteArrayList<>();

    public TestRecord(String testName, String platform) {
        this.testName = testName;
        this.platform = platform;
    }

    public TestAttempt startAttempt() {
        TestAttempt attempt = new TestAttempt(attempts.size() + 1);
        attempts.add(attempt);
        return attempt;
    }

    public void finish() {
        this.endTime = ZonedDateTime.now(ZoneId.systemDefault());
    }

    /** The status of the most recent attempt (i.e. after retries are exhausted). */
    public TestStatus getFinalStatus() {
        if (attempts.isEmpty()) {
            return TestStatus.SKIP;
        }
        return attempts.get(attempts.size() - 1).getStatus();
    }

    public String getTestName() {
        return testName;
    }

    public String getPlatform() {
        return platform;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public List<TestAttempt> getAttempts() {
        return attempts;
    }
}