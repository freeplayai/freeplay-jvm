package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.resources.traces.TraceUpdatePayload;
import ai.freeplay.client.resources.traces.TraceUpdateResponse;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class TracesTest extends HttpClientTestBase {

    @Test
    public void testUpdateTraceWithOutput() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .output(Map.of("result", "updated output"))
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(Map.of("result", "updated output"), requestBody.get("output"));
        });
    }

    @Test
    public void testUpdateTraceWithEvalResults() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Map<String, Object> evalResults = Map.of(
                    "accuracy", 0.95,
                    "valid", true
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .evalResults(evalResults)
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(evalResults, requestBody.get("eval_results"));
            assertFalse(requestBody.containsKey("output"));
        });
    }

    @Test
    public void testUpdateTraceWithOutputAndEvalResults() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .output("new output text")
                            .evalResults(Map.of("score", 0.8))
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals("new output text", requestBody.get("output"));
            assertEquals(Map.of("score", 0.8), requestBody.get("eval_results"));
        });
    }

    @Test
    public void testUpdateTraceWithMetadata() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Map<String, Object> metadata = Map.of("env", "production", "version", 2);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .metadata(metadata)
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(metadata, requestBody.get("metadata"));
            assertFalse(requestBody.containsKey("output"));
            assertFalse(requestBody.containsKey("eval_results"));
        });
    }

    @Test
    public void testUpdateTraceWithFeedback() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Map<String, Object> feedback = Map.of("freeplay_feedback", "positive", "custom_score", 0.9);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .feedback(feedback)
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(feedback, requestBody.get("feedback"));
            assertFalse(requestBody.containsKey("output"));
        });
    }

    @Test
    public void testUpdateTraceWithTestRunInfo() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();
            String testRunId = UUID.randomUUID().toString();
            String testCaseId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .testRunInfo(testRunId, testCaseId)
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            @SuppressWarnings("unchecked")
            Map<String, Object> testRunInfo = (Map<String, Object>) requestBody.get("test_run_info");
            assertNotNull(testRunInfo);
            assertEquals(testRunId, testRunInfo.get("test_run_id"));
            assertEquals(testCaseId, testRunInfo.get("test_case_id"));
            assertFalse(requestBody.containsKey("output"));
        });
    }

    @Test
    public void testUpdateTraceWithAllFields() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();
            String testRunId = UUID.randomUUID().toString();
            String testCaseId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceUpdateResponse response = fpClient.traces().update(
                    new TraceUpdatePayload(projectId, sessionId, traceId)
                            .output("updated")
                            .metadata(Map.of("key", "value"))
                            .feedback(Map.of("freeplay_feedback", "negative"))
                            .evalResults(Map.of("accuracy", 0.99))
                            .testRunInfo(testRunId, testCaseId)
            ).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals("updated", requestBody.get("output"));
            assertEquals(Map.of("key", "value"), requestBody.get("metadata"));
            assertEquals(Map.of("freeplay_feedback", "negative"), requestBody.get("feedback"));
            assertEquals(Map.of("accuracy", 0.99), requestBody.get("eval_results"));
            @SuppressWarnings("unchecked")
            Map<String, Object> testRunInfo = (Map<String, Object>) requestBody.get("test_run_info");
            assertEquals(testRunId, testRunInfo.get("test_run_id"));
            assertEquals(testCaseId, testRunInfo.get("test_case_id"));
        });
    }

    @Test
    public void testUpdateTraceRequiresAtLeastOneField() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            assertThrows(
                    FreeplayClientException.class,
                    () -> fpClient.traces().update(
                            new TraceUpdatePayload(projectId, sessionId, traceId)
                    )
            );
        });
    }

    @Test
    public void testUpdateTraceNotFound() {
        withMockedClient((HttpClient mockedClient) -> {
            try {
                when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/[^/]*/traces/id/[^/]*$"))
                        .thenReturn(asyncResponse(404, "{\"error\": \"Trace not found\"}"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            try {
                fpClient.traces().update(
                        new TraceUpdatePayload(projectId, sessionId, traceId)
                                .output("test")
                ).get();
                fail("Expected FreeplayClientException to be thrown");
            } catch (Exception e) {
                assertTrue(e.getCause() instanceof FreeplayClientException);
                assertTrue(e.getCause().getMessage().contains("404"));
            }
        });
    }

    @Test
    public void testUpdateTraceServerError() {
        withMockedClient((HttpClient mockedClient) -> {
            try {
                when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/[^/]*/traces/id/[^/]*$"))
                        .thenReturn(asyncResponse(500, "{\"error\": \"Internal server error\"}"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> fpClient.traces().update(
                            new TraceUpdatePayload(projectId, sessionId, traceId)
                                    .evalResults(Map.of("score", 1.0))
                    ).get()
            );

            assertTrue(exception.getMessage().contains("500") ||
                      exception.getCause().getMessage().contains("500"));
        });
    }
}
