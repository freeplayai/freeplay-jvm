package ai.freeplay.client.resources.agentdatasets;

import ai.freeplay.client.internal.CallSupport;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AgentDatasets {
    private final CallSupport callSupport;

    public AgentDatasets(CallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<AgentDataset> create(String projectId, CreateAgentDatasetRequest request) {
        return callSupport.createAgentDataset(projectId, request);
    }

    public CompletableFuture<AgentDatasetList> list(String projectId) {
        return callSupport.listAgentDatasets(projectId, 1, 25, null, null);
    }

    public CompletableFuture<AgentDatasetList> list(String projectId, int page, int pageSize) {
        return callSupport.listAgentDatasets(projectId, page, pageSize, null, null);
    }

    public CompletableFuture<AgentDatasetList> list(String projectId, int page, int pageSize, String idFilter, String nameFilter) {
        return callSupport.listAgentDatasets(projectId, page, pageSize, idFilter, nameFilter);
    }

    public CompletableFuture<AgentDataset> get(String projectId, String datasetId) {
        return callSupport.getAgentDataset(projectId, datasetId);
    }

    public CompletableFuture<AgentDataset> update(String projectId, String datasetId, UpdateAgentDatasetRequest request) {
        return callSupport.updateAgentDataset(projectId, datasetId, request);
    }

    public CompletableFuture<Void> delete(String projectId, String datasetId) {
        return callSupport.deleteAgentDataset(projectId, datasetId);
    }

    public CompletableFuture<AgentTestCaseList> listTestCases(String projectId, String datasetId) {
        return callSupport.listAgentTestCases(projectId, datasetId, 1, 25);
    }

    public CompletableFuture<AgentTestCaseList> listTestCases(String projectId, String datasetId, int page, int pageSize) {
        return callSupport.listAgentTestCases(projectId, datasetId, page, pageSize);
    }

    public CompletableFuture<AgentTestCase> getTestCase(String projectId, String datasetId, String testCaseId) {
        return callSupport.getAgentTestCase(projectId, datasetId, testCaseId);
    }

    public CompletableFuture<List<AgentTestCase>> createTestCases(String projectId, String datasetId, List<AgentTestCaseInput> testCases) {
        return callSupport.bulkCreateAgentTestCases(projectId, datasetId, testCases);
    }

    public CompletableFuture<AgentTestCase> updateTestCase(String projectId, String datasetId, String testCaseId, UpdateAgentTestCaseRequest request) {
        return callSupport.updateAgentTestCase(projectId, datasetId, testCaseId, request);
    }

    public CompletableFuture<Void> deleteTestCase(String projectId, String datasetId, String testCaseId) {
        return callSupport.deleteAgentTestCase(projectId, datasetId, testCaseId);
    }

    public CompletableFuture<Void> deleteTestCases(String projectId, String datasetId, List<String> testCaseIds) {
        return callSupport.bulkDeleteAgentTestCases(projectId, datasetId, testCaseIds);
    }
}
