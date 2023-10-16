package ai.freeplay.client;

import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.TestRun;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Map;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static org.junit.Assert.assertEquals;

public class OpenAITestRunTest extends HttpClientTestBase {

    @Test
    public void testRunCreated() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRun(mockedClient);

            String environment = "prod";
            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
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
    public void recordsTestRunId() {
        String templateName = "my-prompt";
        String chatCompletion1 = "\\n\\nSorry, I will try to help";
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockCreateTestRun(mockedClient);
            mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
            mockOpenAIChatCall(mockedClient, chatCompletion1);

            String environment = "prod";
            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            TestRun testRun = fpClient.createTestRun(
                    projectId,
                    environment,
                    testListName
            );
            CompletionSession session = testRun.createSession();
            CompletionResponse completion = session.getCompletion(templateName, testRun.getInputs().get(0));

            assertEquals(unescapeExpected(chatCompletion1), completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 5, 4);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(testRun.getTestRunId(), recordBodyMap.get("test_run_id"));
        });
    }
}
