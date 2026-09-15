package reporting.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import enums.StepStatus;

/**
 * A single logged step (utils.Log call) within a test attempt, with an optional
 * screenshot.
 */
public class StepLog {

    private final StepStatus status;
    private final String message;
    private final String screenshotPath;
    private final ZonedDateTime timestamp = ZonedDateTime.now(ZoneId.systemDefault());

    public StepLog(StepStatus status, String message, String screenshotPath) {
        this.status = status;
        this.message = message;
        this.screenshotPath = screenshotPath;
    }

    public StepStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
}