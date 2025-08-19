package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.resources.recordings.TestRunInfo;
import ai.freeplay.client.thin.resources.sessions.Session;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;
import ai.freeplay.client.thin.resources.testruns.CompletionTestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import ai.freeplay.client.thin.resources.testruns.TestRunRequest;
import ai.freeplay.client.thin.resources.testruns.TraceTestCase;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static ai.freeplay.client.thin.Freeplay.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ThinTraceTestCaseTest extends HttpClientTestBase {

    @Test
    public void testTraceTestCasesCreated() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunWithTraceTestCasesAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRunRequest testRunRequest = fpClient.testRuns()
                    .createRequest(projectId, "agent-dataset")
                    .flavorName("openai_chat")
                    .includeOutputs(true)
                    .build();
            TestRun testRun = fpClient.testRuns().create(testRunRequest).get();

            assertEquals(2, testRun.getTraceTestCases().size());

            TraceTestCase testCase1 = testRun.getTraceTestCases().get(0);
            assertNotNull(testCase1.getTestCaseId());
            assertEquals("Tell me about AI", testCase1.getInput());
            assertEquals("AI is a field of computer science...", testCase1.getOutput());
            assertEquals("value1", testCase1.getCustomMetadata().get("key1"));

            TraceTestCase testCase2 = testRun.getTraceTestCases().get(1);
            assertNotNull(testCase2.getTestCaseId());
            assertEquals("What is machine learning?", testCase2.getInput());
            assertEquals("Machine learning is a subset of AI...", testCase2.getOutput());
            assertEquals("value2", testCase2.getCustomMetadata().get("key2"));
        });
    }

    @Test
    public void testCompletionTestCasesCreated() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunWithCompletionTestCasesAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRunRequest testRunRequest = fpClient.testRuns()
                    .createRequest(projectId, "completion-dataset")
                    .flavorName("openai_chat")
                    .includeOutputs(true)
                    .build();
            TestRun testRun = fpClient.testRuns().create(testRunRequest).get();

            assertEquals(2, testRun.getTestCases().size());

            CompletionTestCase testCase1 = testRun.getTestCases().get(0);
            assertNotNull(testCase1.getTestCaseId());
            assertEquals("Why isn't my sink working?", testCase1.getVariables().get("question"));
            assertEquals("It took PTO today", testCase1.getOutput());
            assertEquals("completion_value1", testCase1.getCustomMetadata().get("completion_key1"));
        });
    }

    @Test
    public void testTraceTestCasesThrowsErrorWhenAccessingCompletionTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunWithTraceTestCasesAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRunRequest testRunRequest = fpClient.testRuns()
                    .createRequest(projectId, "agent-dataset")
                    .flavorName("openai_chat")
                    .includeOutputs(true)
                    .build();
            TestRun testRun = fpClient.testRuns().create(testRunRequest).get();

            FreeplayClientException exception = assertThrows(
                    FreeplayClientException.class,
                    testRun::getTestCases
            );
            assertEquals("Completion test cases are not present. Please use getTraceTestCases() instead.", exception.getMessage());
        });
    }

    @Test
    public void testCompletionTestCasesThrowsErrorWhenAccessingTraceTestCases() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunWithCompletionTestCasesAsync(mockedClient);


            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRunRequest testRunRequest = fpClient.testRuns()
                    .createRequest(projectId, "completion-dataset")
                    .flavorName("openai_chat")
                    .includeOutputs(true)
                    .build();
            TestRun testRun = fpClient.testRuns().create(testRunRequest).get();

            FreeplayClientException exception = assertThrows(
                    FreeplayClientException.class,
                    testRun::getTraceTestCases
            );
            assertEquals("Trace test cases are not present. Please use getCompletionTestCases() instead.", exception.getMessage());
        });
    }

    @Test
    public void testRecordTraceOutputWithTestRunInfo() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordTraceAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            Session session = fpClient.sessions().create();
            TraceInfo traceInfo = session.createTrace("Test input for agent").agentName("test-agent");

            String testRunId = UUID.randomUUID().toString();
            String testCaseId = UUID.randomUUID().toString();
            TestRunInfo testRunInfo = new TestRunInfo(testRunId, testCaseId);

            traceInfo.recordOutput(
                    projectId,
                    "Test output from agent",
                    Map.of("accuracy", 0.95, "valid", true),
                    testRunInfo
            ).get();

            String requestBody = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> payload = JSONUtil.parseMap(requestBody);

            assertEquals("Test input for agent", payload.get("input"));
            assertEquals("Test output from agent", payload.get("output"));
            assertEquals("test-agent", payload.get("agent_name"));

            Map<String, Object> evalResults = (Map<String, Object>) payload.get("eval_results");
            assertEquals(0.95, (Double) evalResults.get("accuracy"), 0.001);
            assertEquals(true, evalResults.get("valid"));

            Map<String, Object> testRunInfoPayload = (Map<String, Object>) payload.get("test_run_info");
            assertEquals(testRunId, testRunInfoPayload.get("test_run_id"));
            assertEquals(testCaseId, testRunInfoPayload.get("test_case_id"));
        });
    }

    @Test
    public void testRecordTraceOutputWithoutTestRunInfo() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordTraceAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            Session session = fpClient.sessions().create();
            TraceInfo traceInfo = session.createTrace("Test input for agent").agentName("test-agent");

            traceInfo.recordOutput(
                    projectId,
                    "Test output from agent",
                    Map.of("accuracy", 0.95)
            ).get();

            String requestBody = getCapturedAsyncBody(mockedClient, 1, 0);
            Map<String, Object> payload = JSONUtil.parseMap(requestBody);

            assertEquals("Test input for agent", payload.get("input"));
            assertEquals("Test output from agent", payload.get("output"));
            assertEquals("test-agent", payload.get("agent_name"));
            assertNull(payload.get("test_run_info"));
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedTestCaseStillWorks() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunWithCompletionTestCasesAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRun testRun = fpClient.testRuns().create(
                    projectId,
                    "completion-dataset",
                    true
            ).get();

            assertEquals(2, testRun.getTestCases().size());
            assertEquals("Why isn't my sink working?", testRun.getTestCases().get(0).getVariables().get("question"));
        });
    }

    // Helper methods for mocking
    private void mockCreateTestRunWithTraceTestCasesAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/test-runs"))
                    .thenReturn(
                            asyncResponse(201, getTestRunTraceTestCasesResponsePayload(UUID.randomUUID().toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockCreateTestRunWithCompletionTestCasesAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/test-runs"))
                    .thenReturn(
                            asyncResponse(201, getTestRunCompletionTestCasesResponsePayload(UUID.randomUUID().toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockRecordTraceAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/sessions/[^/]*/traces/id/[^/]*"))
                    .thenReturn(asyncResponse(201, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getTestRunTraceTestCasesResponsePayload(String testRunId) {
        return JSONUtil.asString(
                object(
                        "test_run_id", testRunId,
                        "trace_test_cases", array(
                                object(
                                        "test_case_id", UUID.randomUUID(),
                                        "input", "Tell me about AI",
                                        "output", "AI is a field of computer science...",
                                        "custom_metadata", object("key1", "value1")
                                ),
                                object(
                                        "test_case_id", UUID.randomUUID(),
                                        "input", "What is machine learning?",
                                        "output", "Machine learning is a subset of AI...",
                                        "custom_metadata", object("key2", "value2")
                                )
                        )
                ));
    }

    private String getTestRunCompletionTestCasesResponsePayload(String testRunId) {
        return JSONUtil.asString(
                object(
                        "test_run_id", testRunId,
                        "test_cases", array(
                                object(
                                        "test_case_id", UUID.randomUUID(),
                                        "variables", object("question", "Why isn't my sink working?"),
                                        "output", "It took PTO today",
                                        "custom_metadata", object("completion_key1", "completion_value1")
                                ),
                                object(
                                        "test_case_id", UUID.randomUUID(),
                                        "variables", object("question", "Why isn't my internet working?"),
                                        "output", "It's playing golf with the sink",
                                        "custom_metadata", object("completion_key2", "completion_value2")
                                )
                        )
                ));
    }
}