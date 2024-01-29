package ai.freeplay.client.thin.resources.testruns;

import java.util.List;

public class TestRun {
    private final String testRunId;
    private final List<TestCase> testCases;

    public TestRun(String testRunId, List<TestCase> testCases) {
        this.testRunId = testRunId;
        this.testCases = testCases;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    @Override
    public String toString() {
        return "TestRun{" +
                "testRunId='" + testRunId + '\'' +
                ", testCases=" + testCases +
                '}';
    }
}
