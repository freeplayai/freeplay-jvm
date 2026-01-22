package ai.freeplay.client;

import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.TestRun;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.RecordProcessor.DO_NOT_RECORD_PROCESSOR;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.MockMethods.routeNotCalled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OpenAITestRunTest extends HttpClientTestBase {

    @Test
    public void testRunCreated() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRun(mockedClient);

            String environment = "prod";
            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );
            TestRun testRun = fpClient.createTestRun(
                    projectId,
                    environment,
                    testListName
            );

            // Completion
            assertEquals(projectId, testRun.getProjectId());
            assertEquals(2, testRun.getInputs().size());
            assertEquals(environment, testRun.getEnvironment());
            assertEquals("Why isn't my sink working?", testRun.getInputs().get(0).get("question"));
            assertEquals("Why isn't my internet working?", testRun.getInputs().get(1).get("question"));
        });
    }

    @Test
    public void recordsTestRunIdAndMetadata() {
        String templateName = "my-prompt";
        String chatCompletion1 = "\\n\\nSorry, I will try to help";
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRun(mockedClient);
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);

            String environment = "prod";
            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );
            TestRun testRun = fpClient.createTestRun(
                    projectId,
                    environment,
                    testListName
            );
            CompletionSession session = testRun.createSession(Map.of("customer_id", 123));
            CompletionResponse completion = session.getCompletion(templateName, testRun.getInputs().get(0));

            assertEquals(unescapeExpected(chatCompletion1), completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(testRun.getTestRunId(), recordBodyMap.get("test_run_id"));
            assertEquals(Map.of("customer_id", 123), recordBodyMap.get("custom_metadata"));
        });
    }

    @Test
    public void doesNotRecordWhenAskedNotTo() {
        String templateName = "my-prompt";
        String chatCompletion1 = "\\n\\nSorry, I will try to help";
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRun(mockedClient);
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);

            String environment = "prod";
            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey)),
                    DO_NOT_RECORD_PROCESSOR);

            TestRun testRun = fpClient.createTestRun(
                    projectId,
                    environment,
                    testListName
            );
            CompletionSession session = testRun.createSession();
            CompletionResponse completion = session.getCompletion(templateName, testRun.getInputs().get(0));

            assertEquals(unescapeExpected(chatCompletion1), completion.getContent());

            // Record call
            assertTrue(routeNotCalled(mockedClient, 3, "record"));
        });
    }
}
