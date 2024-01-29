package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.concurrent.CompletableFuture;

public class TestRuns {
    private final ThinCallSupport callSupport;

    public TestRuns(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<TestRun> create(String projectId, String testList) {
        return callSupport.createTestRun(projectId, testList);
    }
}
