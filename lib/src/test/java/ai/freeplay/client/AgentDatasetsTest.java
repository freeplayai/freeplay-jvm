package ai.freeplay.client;

import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.resources.agentdatasets.*;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedAsyncBody;
import static org.junit.Assert.*;

public class AgentDatasetsTest extends HttpClientTestBase {

    @Test
    public void testCreateAgentDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockCreateAgentDatasetAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            CreateAgentDatasetRequest request = new CreateAgentDatasetRequest("My Agent Dataset")
                    .description("An agent dataset")
                    .compatibleAgentIds(List.of("agent-1"));

            AgentDataset result = fpClient.agentDatasets().create(projectId, request).get();

            assertEquals(datasetId, result.getId());
            assertEquals("My Agent Dataset", result.getName());
            assertEquals("An agent dataset", result.getDescription());
            assertEquals(List.of("agent-1"), result.getCompatibleAgentIds());

            String body = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> bodyMap = JSONUtil.parseMap(body);
            assertEquals("My Agent Dataset", bodyMap.get("name"));
        });
    }

    @Test
    public void testListAgentDatasets() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockListAgentDatasetsAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            AgentDatasetList result = fpClient.agentDatasets().list(projectId).get();

            assertNotNull(result.getData());
            assertEquals(1, result.getData().size());
            assertEquals(datasetId, result.getData().get(0).getId());
            assertEquals("My Agent Dataset", result.getData().get(0).getName());
            assertNotNull(result.getPagination());
            assertEquals(1, result.getPagination().getPage());
            assertFalse(result.getPagination().isHasNext());
        });
    }

    @Test
    public void testGetAgentDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockGetAgentDatasetAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            AgentDataset result = fpClient.agentDatasets().get(projectId, datasetId).get();

            assertEquals(datasetId, result.getId());
            assertEquals("My Agent Dataset", result.getName());
            assertEquals(List.of("agent-1"), result.getCompatibleAgentIds());
        });
    }

    @Test
    public void testUpdateAgentDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            String datasetId = UUID.randomUUID().toString();
            mockUpdateAgentDatasetAsync(mockedClient, datasetId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            UpdateAgentDatasetRequest request = new UpdateAgentDatasetRequest()
                    .name("Updated Agent Dataset");

            AgentDataset result = fpClient.agentDatasets().update(projectId, datasetId, request).get();

            assertEquals(datasetId, result.getId());
            assertEquals("Updated Agent Dataset", result.getName());
        });
    }

    @Test
    public void testDeleteAgentDataset() {
        withMockedClient((HttpClient mockedClient) -> {
            mockDeleteAgentDatasetAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            Void result = fpClient.agentDatasets().delete(projectId, datasetId).get();

            assertNull(result);
        });
    }

    @Test
    public void testListAgentTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockListAgentTestCasesAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            AgentTestCaseList result = fpClient.agentDatasets().listTestCases(projectId, datasetId).get();

            assertNotNull(result.getData());
            assertEquals(1, result.getData().size());
            AgentTestCase tc = result.getData().get(0);
            assertEquals(testCaseId, tc.getId());
            assertNotNull(tc.getInput());
            assertNotNull(tc.getOutput());
            assertEquals("unit-test", tc.getMetadata().get("source"));
        });
    }

    @Test
    public void testGetAgentTestCase() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockGetAgentTestCaseAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            AgentTestCase result = fpClient.agentDatasets().getTestCase(projectId, datasetId, testCaseId).get();

            assertEquals(testCaseId, result.getId());
            assertNotNull(result.getInput());
            assertNotNull(result.getOutput());
            assertEquals("unit-test", result.getMetadata().get("source"));
        });
    }

    @Test
    public void testBulkCreateAgentTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockBulkCreateAgentTestCasesAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            List<AgentTestCaseInput> testCases = List.of(
                    new AgentTestCaseInput()
                            .inputs(Map.of("query", "hello"))
                            .outputs(Map.of("response", "world"))
            );

            List<AgentTestCase> result = fpClient.agentDatasets().createTestCases(projectId, datasetId, testCases).get();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCaseId, result.get(0).getId());

            String body = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> bodyMap = JSONUtil.parseMap(body);
            @SuppressWarnings("unchecked")
            List<Object> data = (List<Object>) bodyMap.get("data");
            assertNotNull(data);
            assertEquals(1, data.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) data.get(0);
            assertNotNull(item.get("inputs"));
            assertNotNull(item.get("outputs"));
        });
    }

    @Test
    public void testUpdateAgentTestCase() {
        withMockedClient((HttpClient mockedClient) -> {
            String testCaseId = UUID.randomUUID().toString();
            mockUpdateAgentTestCaseAsync(mockedClient, testCaseId);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            UpdateAgentTestCaseRequest request = new UpdateAgentTestCaseRequest()
                    .outputs(Map.of("response", "updated world"));

            AgentTestCase result = fpClient.agentDatasets().updateTestCase(projectId, datasetId, testCaseId, request).get();

            assertEquals(testCaseId, result.getId());
        });
    }

    @Test
    public void testDeleteAgentTestCase() {
        withMockedClient((HttpClient mockedClient) -> {
            mockDeleteAgentTestCaseAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            Void result = fpClient.agentDatasets().deleteTestCase(projectId, datasetId, UUID.randomUUID().toString()).get();

            assertNull(result);
        });
    }

    @Test
    public void testBulkDeleteAgentTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            mockBulkDeleteAgentTestCasesAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            String tc1 = UUID.randomUUID().toString();
            String tc2 = UUID.randomUUID().toString();

            Void result = fpClient.agentDatasets().deleteTestCases(projectId, datasetId, List.of(tc1, tc2)).get();

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
