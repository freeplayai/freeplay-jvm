package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.concurrent.CompletableFuture;

public class TestRuns {
    private final ThinCallSupport callSupport;

    public TestRuns(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<TestRun> create(String projectId, String testList) {
        return this.create(projectId, testList, false, null, null);
    }

    public CompletableFuture<TestRun> create(String projectId, String testList, boolean includeOutputs) {
        return this.create(projectId, testList, includeOutputs, null, null);
    }

    public CompletableFuture<TestRun> create(String projectId, String testList, boolean includeOutputs, String name, String description) {
        return callSupport.createTestRun(projectId, testList, includeOutputs, name, description);
    }

    public CompletableFuture<TestRunResults> get(String projectId, String testRunId) {
        return callSupport.getTestRunResults(projectId, testRunId);
    }
}
