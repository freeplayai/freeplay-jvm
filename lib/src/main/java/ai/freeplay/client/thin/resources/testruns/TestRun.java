package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.thin.resources.recordings.TestRunInfo;

import java.util.List;

public class TestRun {
    private final String testRunId;
    private final List<CompletionTestCase> completionTestCases;
    private final List<TraceTestCase> traceTestCases;

    public TestRun(String testRunId, List<CompletionTestCase> completionTestCases, List<TraceTestCase> traceTestCases) {
        this.testRunId = testRunId;
        this.completionTestCases = completionTestCases;
        this.traceTestCases = traceTestCases;
    }

    public String getTestRunId() {
        return testRunId;
    }

    private void mustNotBeBoth() {
        if (completionTestCases != null && !completionTestCases.isEmpty() &&
                traceTestCases != null && !traceTestCases.isEmpty()) {
            throw new FreeplayClientException("Test case and trace test case cannot both be present");
        }
    }

    public List<CompletionTestCase> getTestCases() {
        mustNotBeBoth();

        if (traceTestCases != null && !traceTestCases.isEmpty()) {
            throw new FreeplayClientException("Completion test cases are not present. Please use getTraceTestCases() instead.");
        }

        return completionTestCases != null ? completionTestCases : java.util.Collections.emptyList();
    }

    public List<TraceTestCase> getTraceTestCases() {
        mustNotBeBoth();

        if (completionTestCases != null && !completionTestCases.isEmpty()) {
            throw new FreeplayClientException("Trace test cases are not present. Please use getCompletionTestCases() instead.");
        }

        return traceTestCases != null ? traceTestCases : java.util.Collections.emptyList();
    }

    public TestRunInfo getTestRunInfo(String testCaseId) {
        return new TestRunInfo(testRunId, testCaseId);
    }

    @Override
    public String toString() {
        return "TestRun{" +
                "testRunId='" + testRunId + '\'' +
                ", completionTestCases=" + completionTestCases +
                ", traceTestCases=" + traceTestCases +
                '}';
    }
}
