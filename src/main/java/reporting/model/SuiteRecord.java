package reporting.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Whole-run report data: identity/header info plus every test executed. */
public class SuiteRecord {

    private String productName = "";
    private String teamName = "";
    private String runType = "";
    private String environment = "";
    private int plannedCount;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private final List<TestRecord> tests = new CopyOnWriteArrayList<>();

    public void addTest(TestRecord test) {
        tests.add(test);
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int getPlannedCount() {
        return plannedCount;
    }

    public void setPlannedCount(int plannedCount) {
        this.plannedCount = plannedCount;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public List<TestRecord> getTests() {
        return tests;
    }
}