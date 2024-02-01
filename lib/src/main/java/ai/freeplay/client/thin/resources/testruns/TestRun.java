package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.thin.resources.recordings.TestRunInfo;

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

    public TestRunInfo getTestRunInfo(String testCaseId) {
        return new TestRunInfo(testRunId, testCaseId);
    }

    @Override
    public String toString() {
        return "TestRun{" +
                "testRunId='" + testRunId + '\'' +
                ", testCases=" + testCases +
                '}';
    }
}
