package reporting.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import enums.TestStatus;

/**
 * One execution attempt of a test: attempt 1 is the initial run, attempt 2+ are
 * retries.
 */
public class TestAttempt {

    private final int attemptNumber;
    private final ZonedDateTime startTime = ZonedDateTime.now(ZoneId.systemDefault());
    private ZonedDateTime endTime;
    private TestStatus status = TestStatus.FAIL;
    private String errorMessage;
    private final List<StepLog> steps = new CopyOnWriteArrayList<>();

    public TestAttempt(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void addStep(StepLog step) {
        steps.add(step);
    }

    public void finish(TestStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
        this.endTime = ZonedDateTime.now(ZoneId.systemDefault());
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public TestStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<StepLog> getSteps() {
        return steps;
    }
}