package ai.freeplay.client.resources.testruns;

import ai.freeplay.client.internal.CallSupport;

import java.util.concurrent.CompletableFuture;

public class TestRuns {
    private final CallSupport callSupport;

    public TestRuns(CallSupport callSupport) {
        this.callSupport = callSupport;
    }

    /**
     * Creates a new TestRunRequest.Builder to help construct a test run request.
     */
    public TestRunRequest.Builder createRequest(String projectId, String datasetName) {
        return new TestRunRequest.Builder(projectId, datasetName);
    }

    public CompletableFuture<TestRun> create(TestRunRequest request) {
        return callSupport.createTestRun(
                request.getProjectId(),
                request.getDatasetName(),
                request.includeOutputs(),
                request.getName(),
                request.getDescription(),
                request.getFlavorName(),
                request.getTargetEvaluationIds()
        );
    }

    public CompletableFuture<TestRunResults> get(String projectId, String testRunId) {
        return callSupport.getTestRunResults(projectId, testRunId);
    }
}
