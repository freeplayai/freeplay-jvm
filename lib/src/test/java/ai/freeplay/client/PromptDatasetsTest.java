package ai.freeplay.client;

import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.resources.promptdatasets.*;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedAsyncBody;
import static org.junit.Assert.*;

public class PromptDatasetsTest extends HttpClientTestBase {

    @Test
    public void testCreatePromptDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockCreatePromptDatasetAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            CreatePromptDatasetRequest request = new CreatePromptDatasetRequest("My Dataset")
                    .description("A test dataset")
                    .inputNames(List.of("question"))
                    .supportHistory(false);

            PromptDataset result = fpClient.promptDatasets().create(projectId, request).get();

            assertEquals(datasetId, result.getId());
            assertEquals("My Dataset", result.getName());
            assertEquals("A test dataset", result.getDescription());
            assertEquals(List.of("question"), result.getInputNames());
            assertFalse(result.isSupportHistory());

            String body = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> bodyMap = JSONUtil.parseMap(body);
            assertEquals("My Dataset", bodyMap.get("name"));
            assertEquals("A test dataset", bodyMap.get("description"));
        });
    }

    @Test
    public void testListPromptDatasets() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockListPromptDatasetsAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            PromptDatasetList result = fpClient.promptDatasets().list(projectId).get();

            assertNotNull(result.getData());
            assertEquals(1, result.getData().size());
            assertEquals(datasetId, result.getData().get(0).getId());
            assertEquals("My Dataset", result.getData().get(0).getName());
            assertNotNull(result.getPagination());
            assertEquals(1, result.getPagination().getPage());
            assertFalse(result.getPagination().isHasNext());
        });
    }

    @Test
    public void testGetPromptDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockGetPromptDatasetAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            PromptDataset result = fpClient.promptDatasets().get(projectId, datasetId).get();

            assertEquals(datasetId, result.getId());
            assertEquals("My Dataset", result.getName());
            assertEquals(List.of("question"), result.getInputNames());
        });
    }

    @Test
    public void testUpdatePromptDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockUpdatePromptDatasetAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            UpdatePromptDatasetRequest request = new UpdatePromptDatasetRequest()
                    .name("Updated Dataset");

            PromptDataset result = fpClient.promptDatasets().update(projectId, datasetId, request).get();

            assertEquals(datasetId, result.getId());
            assertEquals("Updated Dataset", result.getName());
        });
    }

    @Test
    public void testDeletePromptDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            mockDeletePromptDatasetAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            Void result = fpClient.promptDatasets().delete(projectId, datasetId).get();

            assertNull(result);
        });
    }

    @Test
    public void testListPromptTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockListPromptTestCasesAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            PromptTestCaseList result = fpClient.promptDatasets().listTestCases(projectId, datasetId).get();

            assertNotNull(result.getData());
            assertEquals(1, result.getData().size());
            PromptTestCase tc = result.getData().get(0);
            assertEquals(testCaseId, tc.getId());
            assertEquals("4", tc.getOutput());
            assertEquals("What is 2+2?", tc.getInputs().get("question"));
            assertEquals("unit-test", tc.getMetadata().get("source"));
        });
    }

    @Test
    public void testGetPromptTestCase() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockGetPromptTestCaseAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            PromptTestCase result = fpClient.promptDatasets().getTestCase(projectId, datasetId, testCaseId).get();

            assertEquals(testCaseId, result.getId());
            assertEquals("4", result.getOutput());
            assertEquals("What is 2+2?", result.getInputs().get("question"));
            assertEquals("unit-test", result.getMetadata().get("source"));
        });
    }

    @Test
    public void testBulkCreatePromptTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockBulkCreatePromptTestCasesAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            List<PromptTestCaseInput> testCases = List.of(
                    new PromptTestCaseInput(Map.of("question", "What is 2+2?")).output("4")
            );

            List<PromptTestCase> result = fpClient.promptDatasets().createTestCases(projectId, datasetId, testCases).get();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCaseId, result.get(0).getId());
            assertEquals("4", result.get(0).getOutput());

            String body = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> bodyMap = JSONUtil.parseMap(body);
            @SuppressWarnings("unchecked")
            List<Object> data = (List<Object>) bodyMap.get("data");
            assertNotNull(data);
            assertEquals(1, data.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) data.get(0);
            assertEquals("4", item.get("output"));
            @SuppressWarnings("unchecked")
            Map<String, Object> inputs = (Map<String, Object>) item.get("inputs");
            assertEquals("What is 2+2?", inputs.get("question"));
        });
    }

    @Test
    public void testUpdatePromptTestCase() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockUpdatePromptTestCaseAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            UpdatePromptTestCaseRequest request = new UpdatePromptTestCaseRequest()
                    .output("updated answer");

            PromptTestCase result = fpClient.promptDatasets().updateTestCase(projectId, datasetId, testCaseId, request).get();

            assertEquals(testCaseId, result.getId());
        });
    }

    @Test
    public void testDeletePromptTestCase() {
        withMockedClient((HttpClient mockedClient) -> {
            mockDeletePromptTestCaseAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            Void result = fpClient.promptDatasets().deleteTestCase(projectId, datasetId, UUID.randomUUID().toString()).get();

            assertNull(result);
        });
    }

    @Test
    public void testBulkDeletePromptTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            mockBulkDeletePromptTestCasesAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            String tc1 = UUID.randomUUID().toString();
            String tc2 = UUID.randomUUID().toString();

            Void result = fpClient.promptDatasets().deleteTestCases(projectId, datasetId, List.of(tc1, tc2)).get();

            assertNull(result);

            String body = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> bodyMap = JSONUtil.parseMap(body);
            @SuppressWarnings("unchecked")
            List<Object> ids = (List<Object>) bodyMap.get("test_case_ids");
            assertNotNull(ids);
            assertEquals(2, ids.size());
            assertTrue(ids.contains(tc1));
            assertTrue(ids.contains(tc2));
        });
    }
}
