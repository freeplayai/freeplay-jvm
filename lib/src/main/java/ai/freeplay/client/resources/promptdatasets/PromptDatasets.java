package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.internal.CallSupport;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PromptDatasets {
    private final CallSupport callSupport;

    public PromptDatasets(CallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<PromptDataset> create(String projectId, CreatePromptDatasetRequest request) {
        return callSupport.createPromptDataset(projectId, request);
    }

    public CompletableFuture<PromptDatasetList> list(String projectId) {
        return callSupport.listPromptDatasets(projectId, 1, 25, null, null);
    }

    public CompletableFuture<PromptDatasetList> list(String projectId, int page, int pageSize) {
        return callSupport.listPromptDatasets(projectId, page, pageSize, null, null);
    }

    public CompletableFuture<PromptDatasetList> list(String projectId, int page, int pageSize, String idFilter, String nameFilter) {
        return callSupport.listPromptDatasets(projectId, page, pageSize, idFilter, nameFilter);
    }

    public CompletableFuture<PromptDataset> get(String projectId, String datasetId) {
        return callSupport.getPromptDataset(projectId, datasetId);
    }

    public CompletableFuture<PromptDataset> update(String projectId, String datasetId, UpdatePromptDatasetRequest request) {
        return callSupport.updatePromptDataset(projectId, datasetId, request);
    }

    public CompletableFuture<Void> delete(String projectId, String datasetId) {
        return callSupport.deletePromptDataset(projectId, datasetId);
    }

    public CompletableFuture<PromptTestCaseList> listTestCases(String projectId, String datasetId) {
        return callSupport.listPromptTestCases(projectId, datasetId, 1, 25);
    }

    public CompletableFuture<PromptTestCaseList> listTestCases(String projectId, String datasetId, int page, int pageSize) {
        return callSupport.listPromptTestCases(projectId, datasetId, page, pageSize);
    }

    public CompletableFuture<PromptTestCase> getTestCase(String projectId, String datasetId, String testCaseId) {
        return callSupport.getPromptTestCase(projectId, datasetId, testCaseId);
    }

    public CompletableFuture<List<PromptTestCase>> createTestCases(String projectId, String datasetId, List<PromptTestCaseInput> testCases) {
        return callSupport.bulkCreatePromptTestCases(projectId, datasetId, testCases);
    }

    public CompletableFuture<PromptTestCase> updateTestCase(String projectId, String datasetId, String testCaseId, UpdatePromptTestCaseRequest request) {
        return callSupport.updatePromptTestCase(projectId, datasetId, testCaseId, request);
    }

    public CompletableFuture<Void> deleteTestCase(String projectId, String datasetId, String testCaseId) {
        return callSupport.deletePromptTestCase(projectId, datasetId, testCaseId);
    }

    public CompletableFuture<Void> deleteTestCases(String projectId, String datasetId, List<String> testCaseIds) {
        return callSupport.bulkDeletePromptTestCases(projectId, datasetId, testCaseIds);
    }
}
