package ai.freeplay.client.thin.resources.recordings;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class TestRunInfo {
    public static TestRunInfo NONE = new TestRunInfo(null, null);

    private final String testRunId;
    private final String testCaseId;

    public TestRunInfo(String testRunId, String testCaseId) {
        this.testRunId = testRunId;
        this.testCaseId = testCaseId;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public String getTestCaseId() {
        return testCaseId;
    }
}
