package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.internal.model.RecordAPIPayload;
import ai.freeplay.client.thin.resources.prompts.*;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.sessions.Session;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import org.junit.Test;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedAsyncBody;
import static ai.freeplay.client.thin.Freeplay.Config;
import static org.junit.Assert.*;

public class ThinClientTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";
    private final Map<String, Object> variables = Map.of("question", "Why isn't my light working?");
    private final Map<String, Object> anthropicLLMParameters = Map.of(
            "model", MODEL_CLAUDE_2,
            "max_tokens", 256
    );
    private final Map<String, Object> openAILLMParameters = Map.of(
            "model", MODEL_GPT_35_TURBO
    );

    @Test
    public void testGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsAsync(mockedClient, templateName, getChatPromptContent(), openAILLMParameters, "openai_chat");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();

            PromptInfo expectedInfo = new PromptInfo(
                    promptTemplateId,
                    promptTemplateVersionId,
                    templateName,
                    "prod",
                    Map.of(),
                    "openai",
                    MODEL_GPT_35_TURBO,
                    "openai_chat"
            );
            assertEquals(expectedInfo, templatePrompt.getPromptInfo());

            List<ChatMessage> expectedMessages = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("assistant", "How may I help you?"),
                    new ChatMessage("user", "{{question}}")
            );
            assertEquals(expectedMessages, templatePrompt.getMessages());
        });
    }

    @Test
    public void testSyntax() {
        withMockedClient((HttpClient mockedClient) -> {

            mockGetPromptsAsync(
                    mockedClient, templateName, getChatPromptContent(), anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            FormattedPrompt<String> anthropicPrompt = templatePrompt
                    .bind(variables)
                    .format(templatePrompt.getPromptInfo().getFlavorName());

            assertEquals("\n\nHuman: You are a support agent." +
                            "\n\nAssistant: How may I help you?" +
                            "\n\nHuman: Why isn't my light working?" +
                            "\n\nAssistant:",
                    anthropicPrompt.getFormattedPrompt());

            // Overriding flavor_name
            FormattedPrompt<List<ChatMessage>> openAIPrompt = templatePrompt
                    .bind(variables)
                    .format("openai_chat");

            List<ChatMessage> expectedMessages = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("assistant", "How may I help you?"),
                    new ChatMessage("user", "Why isn't my light working?")
            );
            assertEquals(expectedMessages, openAIPrompt.getFormattedPrompt());

            // Calling getFormatted instead of chaining manually
            CompletableFuture<FormattedPrompt<List<ChatMessage>>> formattedPrompt = fpClient.prompts().getFormatted(
                    projectId,
                    templateName,
                    "prod",
                    variables,
                    "openai_chat"
            );
            assertEquals(expectedMessages, formattedPrompt.get().getFormattedPrompt());
        });
    }

    @Test
    public void testRecord() {
        withMockedClient((HttpClient mockedClient) -> {
            String expectedPrompt = "[" +
                    "{\"role\":\"system\",\"content\":\"You are a support agent.\"}," +
                    "{\"role\":\"assistant\",\"content\":\"How may I help you?\"}," +
                    "{\"role\":\"user\",\"content\":\"Why isn't my light working?\"}" +
                    "]";
            String completion = "I'd like to help you...";
            long startTime = System.currentTimeMillis();
            long endTime = System.currentTimeMillis() + 5;
            Map<String, Object> customMetadata = Map.of("customer_id", 123);

            mockGetPromptsAsync(mockedClient, templateName, getChatPromptContent(), anthropicLLMParameters, "anthropic_chat");
            mockRecordAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            CompletableFuture<FormattedPrompt<String>> future = fpClient.prompts().getFormatted(
                    projectId, templateName, "prod", variables, "anthropic_chat"
            );
            FormattedPrompt<String> prompt = future.get();

            CallInfo callInfo = CallInfo.from(
                    prompt.getPromptInfo(),
                    startTime,
                    endTime
            ).customMetadata(customMetadata);
            ResponseInfo responseInfo = new ResponseInfo(true);
            List<ChatMessage> allMessages = prompt.allMessages(new ChatMessage("Assistant", completion));

            Session session = fpClient.sessions().create();
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            allMessages,
                            variables,
                            session.getSessionId().toString(),
                            prompt.getPromptInfo(),
                            callInfo,
                            responseInfo
                    ));

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            String requestBody = getCapturedAsyncBody(mockedClient, 2, 1);
            RecordAPIPayload expectedPayload = new RecordAPIPayload(
                    session.getSessionId().toString(),
                    promptTemplateVersionId,
                    promptTemplateId,
                    CallInfo.instantToDouble(Instant.ofEpochMilli(startTime)),
                    CallInfo.instantToDouble(Instant.ofEpochMilli(endTime)),
                    "prod",
                    variables,
                    customMetadata,
                    expectedPrompt,
                    "I'd like to help you...",
                    true,
                    null,
                    "anthropic",
                    MODEL_CLAUDE_2,
                    Map.of("max_tokens", 256)
            );
            RecordAPIPayload apiPayload = JSONUtil.parse(requestBody, RecordAPIPayload.class);
            assertEquals(expectedPayload, apiPayload);
        });
    }

    @Test
    public void testRecordDifferentModelThanPrompt() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            String completion = "I'd like to help you...";
            String differentModel = "different-claude";

            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(differentModel, completion);

            Session session = fpClient.sessions().create();
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            fixtures.getAllMessages(),
                            variables,
                            session.getSessionId().toString(),
                            fixtures.getPromptInfo(),
                            fixtures.getCallInfo(),
                            fixtures.getResponseInfo()
                    ));

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            String expected2 = JSONUtil.toString(fixtures.getBoundPrompt().getMessages());
            RecordAPIPayload expectedPayload = new RecordAPIPayload(
                    session.getSessionId().toString(),
                    promptTemplateVersionId,
                    promptTemplateId,
                    fixtures.getCallInfo().getStartTime(),
                    fixtures.getCallInfo().getEndTime(),
                    "prod",
                    variables,
                    null,
                    expected2,
                    fixtures.getCompletion(),
                    true,
                    null,
                    "anthropic",
                    differentModel,
                    fixtures.getModelParameters()
            );
            RecordAPIPayload actualPayload = JSONUtil.parse(
                    getCapturedAsyncBody(mockedClient, 1, 0), RecordAPIPayload.class
            );
            assertEquals(expectedPayload, actualPayload);
        });
    }

    @Test
    public void testRecordWithoutCompletionId() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordNoCompletionIdAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            String completion = "I'd like to help you...";
            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(MODEL_CLAUDE_2, completion);

            Session session = fpClient.sessions().create();
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            fixtures.getAllMessages(),
                            variables,
                            session.getSessionId().toString(),
                            fixtures.getPromptInfo(),
                            fixtures.getCallInfo(),
                            fixtures.getResponseInfo()
                    ));

            // Assertions
            assertNull(recordFuture.get().getCompletionId());
        });
    }

    @Test
    public void testTestRunCreated() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunAsync(mockedClient);

            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRun testRun = fpClient.testRuns().create(
                    projectId,
                    testListName
            ).get();

            // Completion
            assertEquals(2, testRun.getTestCases().size());
            assertNotNull(testRun.getTestCases().get(0).getTestCaseId());
            assertEquals("Why isn't my sink working?", testRun.getTestCases().get(0).getVariables().get("question"));
            assertNotNull(testRun.getTestCases().get(1).getTestCaseId());
            assertEquals("Why isn't my internet working?", testRun.getTestCases().get(1).getVariables().get("question"));
        });
    }

    @Test
    public void handlesUnauthorizedOnCreateTestRun() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedCreateTestRunAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.testRuns().create(
                            projectId,
                            "core-tests"
                    ).get());
            assertEquals("Error making call [401]", exception.getCause().getMessage());
        });
    }

    @Test
    public void testInvalidFlavorName() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsAsync(
                    mockedClient, templateName, getChatPromptContent(), anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // Calling get() and chaining calls
            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            FreeplayConfigurationException exception = assertThrows(
                    FreeplayConfigurationException.class,
                    () -> templatePrompt
                            .bind(variables)
                            .format("not_a_flavor")
            );
            String expectedExceptionMessage = "Unable to create LLMAdapter for name 'not_a_flavor'.\n";
            assertEquals(expectedExceptionMessage, exception.getMessage());

            // Calling getFormatted()
            ExecutionException exception2 = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.prompts()
                            .getFormatted(projectId, templateName, "prod", Map.of(), "not_a_flavor")
                            .get()
            );
            assertEquals(expectedExceptionMessage, exception2.getCause().getMessage());
        });
    }

    @Test
    public void handlesUnauthorizedOnGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedGetPromptsAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.prompts().get(
                            projectId,
                            "my-prompt",
                            "latest"
                    ).get());
            assertEquals("Error making call [401]", exception.getCause().getMessage());
        });
    }

    @Test
    public void handlesUnauthorizedOnRecord() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedRecordAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(MODEL_CLAUDE_2, "Some completion");

            Session session = fpClient.sessions().create();
            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.recordings().create(
                            new RecordInfo(
                                    fixtures.getAllMessages(),
                                    variables,
                                    session.getSessionId().toString(),
                                    fixtures.getPromptInfo(),
                                    fixtures.getCallInfo(),
                                    fixtures.getResponseInfo()
                            )).get());
            assertEquals("Error making call [401]", exception.getCause().getMessage());
        });
    }

    private class StubbedRecordFixtures {
        private final BoundPrompt boundPrompt;
        private final String completion;

        private final PromptInfo promptInfo;
        private final CallInfo callInfo;
        private final ResponseInfo responseInfo = new ResponseInfo(true);
        private final Map<String, Object> modelParameters;

        public StubbedRecordFixtures(
                String model,
                String completion
        ) {
            modelParameters = Map.of("max_tokens", 256);
            promptInfo = new PromptInfo(
                    promptTemplateId,
                    promptTemplateVersionId,
                    templateName,
                    "prod",
                    modelParameters,
                    "anthropic",
                    MODEL_CLAUDE_2,
                    "anthropic_chat"
            );
            this.completion = completion;

            callInfo = new CallInfo(
                    "anthropic",
                    model,
                    System.currentTimeMillis() - 10,
                    System.currentTimeMillis(),
                    modelParameters
            );

            boundPrompt = new TemplatePrompt(
                    getPromptInfo(),
                    List.of(
                            new ChatMessage("system", "You are a support agent."),
                            new ChatMessage("assistant", "How may I help you?"),
                            new ChatMessage("user", "{{question}}")
                    )).bind(variables);
        }

        public String getCompletion() {
            return completion;
        }

        public Map<String, Object> getModelParameters() {
            return modelParameters;
        }

        public PromptInfo getPromptInfo() {
            return promptInfo;
        }

        public CallInfo getCallInfo() {
            return callInfo;
        }

        public ResponseInfo getResponseInfo() {
            return responseInfo;
        }

        public BoundPrompt getBoundPrompt() {
            return boundPrompt;
        }

        public List<ChatMessage> getAllMessages() {
            return getBoundPrompt()
                    .format("anthropic_chat")
                    .allMessages(new ChatMessage("Assistant", completion));
        }
    }
}
