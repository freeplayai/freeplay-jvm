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
    public TestRunRequest.Builder createRequest(String projectId, String testList) {
        return new TestRunRequest.Builder(projectId, testList);
    }

    /**
     * @deprecated Use {@link #createRequest(String, String)} instead to construct a request
     */
    @Deprecated
    public CompletableFuture<TestRun> create(String projectId, String testList) {
        return create(new TestRunRequest.Builder(projectId, testList).build());
    }

    /**
     * @deprecated Use {@link #createRequest(String, String)} instead to construct a request
     */
    @Deprecated
    public CompletableFuture<TestRun> create(String projectId, String testList, boolean includeOutputs) {
        return create(new TestRunRequest.Builder(projectId, testList)
                .includeOutputs(includeOutputs)
                .build());
    }

    /**
     * @deprecated Use {@link #createRequest(String, String)} instead to construct a request
     */
    @Deprecated
    public CompletableFuture<TestRun> create(String projectId, String testList, boolean includeOutputs, String name, String description) {
        return create(new TestRunRequest.Builder(projectId, testList)
                .includeOutputs(includeOutputs)
                .name(name)
                .description(description)
                .build());
    }

    public CompletableFuture<TestRun> create(TestRunRequest request) {
        return callSupport.createTestRun(
            request.getProjectId(),
            request.getTestList(),
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
