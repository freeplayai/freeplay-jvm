package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.internal.dto.RecordDTO;
import ai.freeplay.client.thin.internal.dto.TraceInfoDTO;
import ai.freeplay.client.thin.internal.ThinCallSupport;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.thin.resources.feedback.CustomerFeedbackResponse;
import ai.freeplay.client.thin.resources.feedback.TraceFeedbackResponse;
import ai.freeplay.client.thin.resources.prompts.*;
import ai.freeplay.client.thin.resources.recordings.*;
import ai.freeplay.client.thin.resources.sessions.Session;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;
import ai.freeplay.client.thin.resources.sessions.SpanKind;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import ai.freeplay.client.thin.resources.testruns.TestRunRequest;
import ai.freeplay.client.thin.resources.testruns.TestRunResults;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedAsyncBody;
import static ai.freeplay.client.thin.Freeplay.Config;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class ThinClientTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";
    private final Map<String, Object> variables = Map.of("question", "Why isn't my light working?");
    private final Map<String, Object> anthropicLLMParameters = Map.of(
            "model", MODEL_CLAUDE_2,
            "max_tokens", 256
    );
    private final Map<String, Object> sagemakerLLMParameters = Map.of(
            "max_new_tokens", 256,
            "temperature", 0.1
    );
    private final Map<String, Object> openAILLMParameters = Map.of(
            "model", MODEL_GPT_35_TURBO
    );

    @Test
    public void testGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatPromptContentObjects(), openAILLMParameters, "openai_chat"
            );

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
            ).providerInfo(Map.of("provider", "info"));
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
            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatPromptContentObjects(), anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            FormattedPrompt<List<ChatMessage>> anthropicPrompt = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format(templatePrompt.getPromptInfo().getFlavorName());

            List<ChatMessage> expectedMessages = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("assistant", "How may I help you?"),
                    new ChatMessage("user", "Why isn't my light working?")
            );
            List<ChatMessage> expectedMessagesWithoutSystem = List.of(
                    new ChatMessage("assistant", "How may I help you?"),
                    new ChatMessage("user", "Why isn't my light working?")
            );
            assertEquals(expectedMessagesWithoutSystem, anthropicPrompt.getFormattedPrompt());
            assertTrue(anthropicPrompt.getSystemContent().isPresent());
            assertEquals("You are a support agent.", anthropicPrompt.getSystemContent().get());

            // Overriding flavor_name
            FormattedPrompt<List<ChatMessage>> openAIPrompt = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format("openai_chat");

            assertEquals(expectedMessages, openAIPrompt.getFormattedPrompt());
            assertTrue(openAIPrompt.getSystemContent().isPresent());
            assertEquals("You are a support agent.", openAIPrompt.getSystemContent().get());

            // Calling getFormatted instead of chaining manually
            CompletableFuture<FormattedPrompt<List<ChatMessage>>> formattedPrompt = fpClient.prompts().getFormatted(
                    projectId,
                    templateName,
                    "prod",
                    variables,
                    "openai_chat"
            );
            assertEquals(expectedMessages, formattedPrompt.get().getFormattedPrompt());

            // Baseten Mistral (effectively OpenAI's format)
            CompletableFuture<FormattedPrompt<List<ChatMessage>>> basetenMistralPrompt = fpClient.prompts().getFormatted(
                    projectId,
                    templateName,
                    "prod",
                    variables,
                    "baseten_mistral_chat"
            );
            assertEquals(expectedMessages, basetenMistralPrompt.get().getFormattedPrompt());

            // Gemini
            CompletableFuture<FormattedPrompt<List<Content>>> geminiPrompt = fpClient.prompts().getFormatted(
                    projectId,
                    templateName,
                    "prod",
                    variables,
                    "gemini_chat"
            );
            assertEquals(List.of(
                    ContentMaker.forRole("model").fromString("How may I help you?"),
                    ContentMaker.forRole("user").fromString("Why isn't my light working?")
            ), geminiPrompt.get().getFormattedPrompt());
        });
    }

    @Test
    public void testSyntaxLlama3() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptV2Async(
                    mockedClient, templateName, "latest", getChatPromptContentObjects(), sagemakerLLMParameters, "llama_3_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "latest").get();
            FormattedPrompt<String> sagemakerPrompt = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format(templatePrompt.getPromptInfo().getFlavorName());

            assertEquals(
                    "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n" +
                            "You are a support agent.<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n" +
                            "How may I help you?<|eot_id|><|start_header_id|>user<|end_header_id|>\n" +
                            "Why isn't my light working?<|eot_id|><|start_header_id|>assistant<|end_header_id|>",
                    sagemakerPrompt.getFormattedPrompt());
            assertTrue(sagemakerPrompt.getSystemContent().isPresent());
            assertEquals("You are a support agent.", sagemakerPrompt.getSystemContent().get());
        });
    }

    @Test
    public void testSyntaxWithoutSystemMessage() {
        withMockedClient((HttpClient mockedClient) -> {
            List<Object> chatPromptContentObjectsWithoutSystem = array(
                    object(
                            "role", "assistant",
                            "content", "How may I help you?"
                    ),
                    object(
                            "role", "user",
                            "content", "{{question}}"
                    )
            );
            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", chatPromptContentObjectsWithoutSystem, anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            FormattedPrompt<List<ChatMessage>> anthropicPrompt = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format(templatePrompt.getPromptInfo().getFlavorName());

            assertEquals(List.of(
                            new ChatMessage("assistant", "How may I help you?"),
                            new ChatMessage("user", "Why isn't my light working?")
                    ),
                    anthropicPrompt.getFormattedPrompt());
            assertTrue(anthropicPrompt.getSystemContent().isEmpty());
        });
    }

    @Test
    public void testHistory() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatWithHistoryPromptContentObjects(), anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            Map<String, Object> variables = Map.of("number", "2");

            // No history given
            BoundPrompt boundPrompt = templatePrompt.bind(new TemplatePrompt.BindRequest(variables));
            List<ChatMessage> expectedMessages = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("user", "User message 2")
            );
            assertEquals(expectedMessages, boundPrompt.getMessages());

            // History given
            BoundPrompt boundPrompt2 = templatePrompt.bind(new TemplatePrompt.BindRequest(variables).history(List.of(
                    new ChatMessage("user", "User message 1"),
                    new ChatMessage("assistant", "assistant message 1")
            )));
            List<ChatMessage> expectedMessages2 = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("user", "User message 1"),
                    new ChatMessage("assistant", "assistant message 1"),
                    new ChatMessage("user", "User message 2")
            );
            assertEquals(expectedMessages2, boundPrompt2.getMessages());
        });
    }

    @Test
    public void testUnexpectedHistory() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatPromptContentObjects(), anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            Map<String, Object> variables = Map.of("number", "2");

            // No history placeholder in the prompt
            try {
                templatePrompt.bind(new TemplatePrompt.BindRequest(variables).history(List.of(
                        new ChatMessage("user", "User message 1")
                )));
                fail("Should have gotten an exception");
            } catch (Exception e) {
                assertEquals(
                        "Received history but prompt 'my-prompt' does not have a history placeholder.",
                        e.getMessage()
                );
            }
        });
    }

    @Test
    public void testRecord() {
        withMockedClient((HttpClient mockedClient) -> {
            String completion = "I'd like to help you...";
            long startTime = System.currentTimeMillis();
            long endTime = System.currentTimeMillis() + 5;
            Map<String, Object> customMetadata = Map.of("customer_id", 123);

            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatPromptContentObjects(), anthropicLLMParameters, "anthropic_chat"
            );
            mockRecordAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            CompletableFuture<FormattedPrompt<String>> future = fpClient.prompts().getFormatted(
                    projectId, templateName, "prod", variables, "anthropic_chat"
            );
            FormattedPrompt<String> prompt = future.get();

            CallInfo.UsageTokens usageTokens = new CallInfo.UsageTokens(123, 456);
            CallInfo.ApiStyle apiStyle = CallInfo.ApiStyle.BATCH;
            CallInfo callInfo = CallInfo.from(
                    prompt.getPromptInfo(),
                    startTime,
                    endTime
            ).usage(usageTokens).apiStyle(apiStyle);
            ResponseInfo responseInfo = new ResponseInfo(true);
            List<ChatMessage> allMessages = prompt.allMessages(new ChatMessage("Assistant", completion));
            Map<String, Object> evalResults = Map.of("bool_value", true, "float_value", 0.23);

            Session session = fpClient.sessions().create()
                    .customMetadata(customMetadata);

                        RecordInfo recordInfo = new RecordInfo(
                    projectId,
                    allMessages
            ).sessionInfo(session.getSessionInfo())
                    .inputs(variables)
                    .promptVersionInfo(prompt.getPromptInfo())
                    .callInfo(callInfo)
                    .responseInfo(responseInfo)
                    .evalResults(evalResults);
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(recordInfo);

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            String requestBody = getCapturedAsyncBody(mockedClient, 2, 1);
            RecordDTO expectedPayload = new RecordDTO(
                    allMessages,
                    variables,
                    new RecordDTO.SessionInfoDTO(session.getSessionInfo().getSessionId(), session.getSessionInfo().getCustomMetadata()),
                    new RecordDTO.PromptVersionInfoDTO(
                            prompt.getPromptInfo().getPromptTemplateVersionId(),
                            prompt.getPromptInfo().getEnvironment(),
                            projectId),
                    new RecordDTO.CallInfoDTO(
                            callInfo.getProvider(),
                            callInfo.getModel(),
                            callInfo.getStartTime(),
                            callInfo.getEndTime(),
                            callInfo.getModelParameters()
                    ).providerInfo(
                            callInfo.getProviderInfo()
                    ).usage(
                            new RecordDTO.CallInfoDTO.UsageTokensDTO(callInfo.getUsage().getPromptTokens(), callInfo.getUsage().getCompletionTokens())
                    ).apiStyle(CallInfo.ApiStyle.BATCH),
                    new RecordDTO.ResponseInfoDTO(responseInfo.isComplete(), null,
                            responseInfo.getPromptTokens(), responseInfo.getResponseTokens()),
                    null,
                    Map.of("bool_value", true, "float_value", 0.23),
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
            RecordDTO apiPayload = JSONUtil.parse(requestBody, RecordDTO.class);
            assertEquals(expectedPayload, apiPayload);
        });
    }

    @Test
    public void testSessionDelete() {
        withMockedClient((HttpClient mockedClient) -> {
            mockSessionDelete(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            fpClient.sessions().delete(projectId, UUID.randomUUID().toString());

            ArgumentCaptor<HttpRequest> recordRequestArg = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mockedClient).sendAsync(recordRequestArg.capture(), any());
            assertEquals("DELETE", recordRequestArg.getValue().method());
        });
    }

    @Test
    public void testRecordDifferentModelThanPrompt() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            String completion = "I'd like to help you...";
            String differentModel = "different-claude";

            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(
                    "anthropic", differentModel, "anthropic_chat", completion
            );

            Session session = fpClient.sessions().create();
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            fixtures.getAllMessages()
                    ).sessionInfo(session.getSessionInfo())
                    .inputs(variables)
                    .promptVersionInfo(fixtures.getPromptInfo())
                    .callInfo(fixtures.getCallInfo())
                    .responseInfo(fixtures.getResponseInfo()));

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            RecordDTO expectedPayload = new RecordDTO(
                    fixtures.getAllMessages(),
                    variables,
                    new RecordDTO.SessionInfoDTO(session.getSessionId(), session.getCustomMetadata()),
                    new RecordDTO.PromptVersionInfoDTO(
                            fixtures.getPromptInfo().getPromptTemplateVersionId(),
                            fixtures.getPromptInfo().getEnvironment(),
                            projectId),
                    new RecordDTO.CallInfoDTO(fixtures.getCallInfo().getProvider(), fixtures.getCallInfo().getModel(),
                            fixtures.getCallInfo().getStartTime(), fixtures.getCallInfo().getEndTime(), fixtures.getCallInfo().getModelParameters()).providerInfo(fixtures.getCallInfo().getProviderInfo()),
                    new RecordDTO.ResponseInfoDTO(fixtures.getResponseInfo().isComplete(), null,
                            fixtures.getResponseInfo().getPromptTokens(), fixtures.getResponseInfo().getResponseTokens()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
            RecordDTO actualPayload = JSONUtil.parse(
                    getCapturedAsyncBody(mockedClient, 1, 0), RecordDTO.class
            );
            assertEquals(expectedPayload, actualPayload);
        });
    }

    @Test
    public void testRecordTestRunId() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            String testRunId = UUID.randomUUID().toString();
            String testCaseId = UUID.randomUUID().toString();
            String completion = "I'd like to help you...";
            String differentModel = "different-claude";

            mockGetTestRunResults(mockedClient, testRunId);

            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(
                    "anthropic", differentModel, "anthropic_chat", completion
            );

            Session session = fpClient.sessions().create();
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            fixtures.getAllMessages()
                    ).sessionInfo(session.getSessionInfo())
                    .inputs(variables)
                    .promptVersionInfo(fixtures.getPromptInfo())
                    .callInfo(fixtures.getCallInfo())
                    .responseInfo(fixtures.getResponseInfo()).testRunInfo(new TestRunInfo(testRunId, testCaseId)));

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            RecordDTO expectedPayload = new RecordDTO(
                    fixtures.getAllMessages(),
                    variables,
                    new RecordDTO.SessionInfoDTO(session.getSessionInfo().getSessionId(), session.getSessionInfo().getCustomMetadata()),
                    new RecordDTO.PromptVersionInfoDTO(
                            fixtures.getPromptInfo().getPromptTemplateVersionId(),
                            fixtures.getPromptInfo().getEnvironment(),
                            projectId),
                    new RecordDTO.CallInfoDTO(fixtures.getCallInfo().getProvider(), fixtures.getCallInfo().getModel(),
                            fixtures.getCallInfo().getStartTime(), fixtures.getCallInfo().getEndTime(), fixtures.getCallInfo().getModelParameters()).providerInfo(fixtures
                            .getCallInfo().getProviderInfo()),
                    new RecordDTO.ResponseInfoDTO(fixtures.getResponseInfo().isComplete(), null,
                            fixtures.getResponseInfo().getPromptTokens(), fixtures.getResponseInfo().getResponseTokens()),
                    new RecordDTO.TestRunInfoDTO(testRunId, testCaseId),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
            RecordDTO actualPayload = JSONUtil.parse(
                    getCapturedAsyncBody(mockedClient, 1, 0), RecordDTO.class
            );
            assertEquals(expectedPayload, actualPayload);

            CompletableFuture<TestRunResults> testRunResults = fpClient.testRuns().get(projectId, testRunId);

            assertEquals(testRunId, testRunResults.get().getId());
            assertNull(testRunResults.get().getName());
            assertNull(testRunResults.get().getDescription());
        });
    }

    @Test
    public void testRecordWithoutCompletionId() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordNoCompletionIdAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            String completion = "I'd like to help you...";
            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(
                    "anthropic", MODEL_CLAUDE_2, "anthropic_chat", completion
            );

            Session session = fpClient.sessions().create();
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            fixtures.getAllMessages()
                    ).sessionInfo(session.getSessionInfo())
                    .inputs(variables)
                    .promptVersionInfo(fixtures.getPromptInfo())
                    .callInfo(fixtures.getCallInfo())
                    .responseInfo(fixtures.getResponseInfo()));

            // Assertions
            assertNull(recordFuture.get().getCompletionId());
        });
    }

    @Test
    public void testRecordWithInvalidCustomMetadata() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            FreeplayClientException exception = assertThrows(
                    FreeplayClientException.class,
                    () -> fpClient.sessions().create().customMetadata(
                            Map.of("not_allowed", new Object())
                    )
            );

            assertEquals(
                    "Invalid value for key 'not_allowed': Value must be a string, number, or boolean.",
                    exception.getMessage()
            );
        });
    }

    @Test
    public void testRecordTrace() {
        withMockedClient((HttpClient mockedClient) -> {
            String completion = "42";
            String input = "What is the meaning of life?";
            long startTime = System.currentTimeMillis();
            long endTime = System.currentTimeMillis() + 5;
            Map<String, Object> customMetadata = Map.of("customer_id", 123);
            Map<String, Object> traceCustomMetadata = Map.of("bool_field", true);

            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatPromptContentObjects(), anthropicLLMParameters, "anthropic_chat"
            );
            mockRecordAsync(mockedClient);
            mockRecordTraceAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            CompletableFuture<FormattedPrompt<String>> future = fpClient.prompts().getFormatted(
                    projectId, templateName, "prod", variables, "anthropic_chat"
            );
            FormattedPrompt<String> prompt = future.get();

            CallInfo callInfo = CallInfo.from(
                    prompt.getPromptInfo(),
                    startTime,
                    endTime
            );
            ResponseInfo responseInfo = new ResponseInfo(true);
            List<ChatMessage> allMessages = prompt.allMessages(new ChatMessage("Assistant", completion));
            Map<String, Object> evalResults = Map.of("bool_value", true, "float_value", 0.23);
            Map<String, Object> traceEvalResults = Map.of("bool_value", false, "float_value", 0.45);

            Session session = fpClient.sessions().create()
                    .customMetadata(customMetadata);
            TraceInfo traceInfo = session.createTrace(input, "agent_name", traceCustomMetadata);
            traceInfo.kind(SpanKind.TOOL).name("test_tool_span");
            RecordInfo recordInfo = new RecordInfo(
                    projectId,
                    allMessages
            ).sessionInfo(session.getSessionInfo())
                    .inputs(variables)
                    .promptVersionInfo(prompt.getPromptInfo())
                    .callInfo(callInfo)
                    .responseInfo(responseInfo)
                    .evalResults(evalResults)
                    .traceInfo(traceInfo);
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(recordInfo);

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            String requestBody = getCapturedAsyncBody(mockedClient, 2, 1);
            RecordDTO expectedPayload = new RecordDTO(
                    allMessages,
                    variables,
                    new RecordDTO.SessionInfoDTO(session.getSessionInfo().getSessionId(), session.getSessionInfo().getCustomMetadata()),
                    new RecordDTO.PromptVersionInfoDTO(
                            prompt.getPromptInfo().getPromptTemplateVersionId(),
                            prompt.getPromptInfo().getEnvironment(),
                            projectId),
                    new RecordDTO.CallInfoDTO(callInfo.getProvider(), callInfo.getModel(),
                            callInfo.getStartTime(), callInfo.getEndTime(), callInfo.getModelParameters()).providerInfo(callInfo
                            .getProviderInfo()),
                    new RecordDTO.ResponseInfoDTO(responseInfo.isComplete(), null,
                            responseInfo.getPromptTokens(), responseInfo.getResponseTokens()),
                    null,
                    Map.of("bool_value", true, "float_value", 0.23),
                    new RecordDTO.TraceInfoDTO(traceInfo.getTraceId()),
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
            RecordDTO apiPayload = JSONUtil.parse(requestBody, RecordDTO.class);
            assertEquals(expectedPayload, apiPayload);

            traceInfo.recordOutput(projectId, completion, traceEvalResults).get();

            String traceRequestBody = getCapturedAsyncBody(mockedClient, 3, 2);
            TraceInfoDTO expectedTracePayload = new TraceInfoDTO(
                    traceInfo.getInput(),
                    traceInfo.getOutput(),
                    traceInfo.getAgentName(),
                    traceInfo.getCustomMetadata(),
                    traceEvalResults,
                    null,
                    null,
                    SpanKind.TOOL,
                    "test_tool_span",
                    traceInfo.getStartTime(),
                    traceInfo.getEndTime()
            );
            TraceInfoDTO actualTracePayload = JSONUtil.parse(traceRequestBody, TraceInfoDTO.class);
            assertEquals(expectedTracePayload, actualTracePayload);
        });
    }

    @Test
    public void testRecordFunctionCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // No completion response on function calls
            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(
                    "openai", MODEL_GPT_35_TURBO, "openai_chat", null
            );

            Session session = fpClient.sessions().create();
            String functionName = "function_name";
            String arguments = "{\"location\": \"San Francisco, CA\", \"format\": \"celsius\"}";
            ResponseInfo responseInfo = new ResponseInfo(true)
                    .functionCall(new OpenAIFunctionCall(functionName, arguments));
            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            fixtures.getAllMessages()
                    ).sessionInfo(session.getSessionInfo())
                            .inputs(variables)
                            .promptVersionInfo(fixtures.getPromptInfo())
                            .callInfo(fixtures.getCallInfo())
                            .responseInfo(responseInfo));

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());

            RecordDTO expectedPayload = new RecordDTO(
                    fixtures.getAllMessages(),
                    variables,
                    new RecordDTO.SessionInfoDTO(session.getSessionId(), session.getCustomMetadata()),
                    new RecordDTO.PromptVersionInfoDTO(
                            fixtures.getPromptInfo().getPromptTemplateVersionId(),
                            fixtures.getPromptInfo().getEnvironment(),
                            projectId),
                    new RecordDTO.CallInfoDTO(fixtures.getCallInfo().getProvider(), fixtures.getCallInfo().getModel(),
                            fixtures.getCallInfo().getStartTime(), fixtures.getCallInfo().getEndTime(), fixtures.getCallInfo().getModelParameters()).providerInfo(fixtures
                            .getCallInfo().getProviderInfo()),
                    new RecordDTO.ResponseInfoDTO(responseInfo.isComplete(), new RecordDTO.OpenAIFunctionCallDTO(responseInfo.getFunctionCall().getName(), responseInfo.getFunctionCall().getArguments()),
                            responseInfo.getPromptTokens(), responseInfo.getResponseTokens()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of()
            );
            RecordDTO actualPayload = JSONUtil.parse(
                    getCapturedAsyncBody(mockedClient, 1, 0), RecordDTO.class
            );
            assertEquals(expectedPayload, actualPayload);
        });
    }

    @Test
    public void testRecordWithStructuredToolCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            List<Object> toolCall = List.of(Map.of(
                    "type", "tool_use",
                    "id", "toolu_01A09q90qw90lq917835lq9",
                    "name", "get_weather",
                    "input", Map.of(
                            "location", "San Francisco",
                            "unit", "celsius"
                    )
            ));
            ChatMessage toolCallMessage = new ChatMessage("assistant", toolCall);

            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(
                    "openai", MODEL_GPT_35_TURBO, "openai_chat", null
            );

            List<ChatMessage> messages = fixtures.getBoundPrompt()
                    .format(fixtures.getPromptInfo().getFlavorName())
                    .allMessages(toolCallMessage);

            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            messages
                    ).inputs(variables)
                            .promptVersionInfo(fixtures.getPromptInfo())
                            .callInfo(fixtures.getCallInfo())
                            .responseInfo(fixtures.getResponseInfo())
                            .toolSchema(fixtures.getToolSchema()));

            // Assertions
            assertNotNull(recordFuture.get().getCompletionId());
            Map<String, Object> request = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));

            // Assert that the last message in the request is the tool call message
            List<Map<String, Object>> requestMessages = (List<Map<String, Object>>) request.get("messages");
            Map<String, Object> lastMessage = requestMessages.get(requestMessages.size() - 1);
            assertEquals("assistant", lastMessage.get("role"));
            assertEquals(toolCall, lastMessage.get("content"));

            // Assert that the tool schema is included in the request
            assertEquals(fixtures.getToolSchema(), request.get("tool_schema"));
        });
    }

    @Test
    public void testRecordWithCompletionToolCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockRecordAsync(mockedClient);
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // Create a completion message with tool call
            Map<String, Object> toolCallCompletion = Map.of(
                    "role", "assistant",
                    "tool_calls", List.of(Map.of(
                            "id", "call_123",
                            "type", "function",
                            "name", "get_weather",
                            "arguments", Map.of(
                                    "location", "San Francisco"
                            )
                    ))
            );
            ChatMessage toolCallMessage = new ChatMessage(toolCallCompletion);

            StubbedRecordFixtures fixtures = new StubbedRecordFixtures(
                    "openai", MODEL_GPT_35_TURBO, "openai_chat", null
            );

            List<ChatMessage> messages = fixtures.getBoundPrompt()
                    .format(fixtures.getPromptInfo().getFlavorName())
                    .allMessages(toolCallMessage);

            CompletableFuture<RecordResponse> recordFuture = fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            messages
                    ).inputs(variables)
                            .promptVersionInfo(fixtures.getPromptInfo())
                            .callInfo(fixtures.getCallInfo())
                            .responseInfo(fixtures.getResponseInfo())
                            .toolSchema(fixtures.getToolSchema()));

            assertNotNull(recordFuture.get().getCompletionId());
            Map<String, Object> request = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertTrue(((List<Map<String, Object>>) request.get("messages")).contains(toolCallCompletion));
            assertTrue(request.get("tool_schema").equals(fixtures.getToolSchema()));
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
                    testListName,
                    true
            ).get();

            // Completion
            assertEquals(2, testRun.getTestCases().size());
            assertNotNull(testRun.getTestCases().get(0).getTestCaseId());
            assertEquals("Why isn't my sink working?", testRun.getTestCases().get(0).getVariables().get("question"));
            assertEquals("It took PTO today", testRun.getTestCases().get(0).getOutput());
            assertNotNull(testRun.getTestCases().get(1).getTestCaseId());
            assertEquals("Why isn't my internet working?", testRun.getTestCases().get(1).getVariables().get("question"));
            assertEquals("It's playing golf with the sink", testRun.getTestCases().get(1).getOutput());
        });
    }

    @Test
    public void testTestRunCreatedWithNameAndDescription() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunAsync(mockedClient);

            String testListName = "core-tests";
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            TestRun testRun = fpClient.testRuns().create(
                    projectId,
                    testListName,
                    true,
                    "test-run-name",
                    "test-run-description"
            ).get();

            // Completion
            assertEquals(2, testRun.getTestCases().size());
        });
    }

    @Test
    public void testCustomerFeedback() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateCustomerFeedbackAsync(mockedClient);
            String completionId = UUID.randomUUID().toString();
            Map<String, Object> feedback = Map.of(
                    "helpful", "thumbsup",
                    "intkey", 1234,
                    "floatkey", 12.34,
                    "booleankey", false
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            CustomerFeedbackResponse response = fpClient.customerFeedback().update(projectId, completionId, feedback).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(feedback, requestBody);
        });
    }

    @Test
    public void testTraceFeedback() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceFeedbackAsync(mockedClient);
            String traceId = UUID.randomUUID().toString();
            Map<String, Object> feedback = Map.of(
                    "helpful", "thumbsup",
                    "intkey", 1234,
                    "floatkey", 12.34,
                    "booleankey", false,
                    "freeplay_feedback", "positive"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TraceFeedbackResponse response = fpClient.customerFeedback().updateTrace(projectId, traceId, feedback).get();

            assertNotNull(response);

            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(feedback, requestBody);
        });
    }

    @Test
    public void testHandlesUnauthorizedOnCustomerFeedback() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedUpdateCustomerFeedbackAsync(mockedClient);

            String completionId = UUID.randomUUID().toString();
            Map<String, Object> feedback = Map.of(
                    "helpful", "thumbsup",
                    "intkey", 1234,
                    "floatkey", 12.34,
                    "booleankey", false
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.customerFeedback().update(projectId, completionId, feedback).get()
            );
            assertEquals("Error making call [401]", exception.getCause().getMessage());
        });
    }

    @Test
    public void testCallInfoPrecisionWithLeadingZeros() {
        long startTime = 1749762235009L; // 009 milliseconds
        long endTime = 1749762235339L;   // 339 milliseconds

        Map<String, Object> modelParams = Map.of("model", "test-model");

        CallInfo callInfo = new CallInfo(
                "test-provider",
                "test-model",
                startTime,
                endTime,
                modelParams
        );

        // What the values should be (in seconds with proper decimal places)
        double expectedStartTime = startTime / 1000.0; // 1749762235.009
        double expectedEndTime = endTime / 1000.0;     // 1749762235.339

        assertEquals("Start time should preserve leading zero milliseconds (1749762235.009)",
                expectedStartTime, callInfo.getStartTime(), 0.0001);
        assertEquals("End time should be correct (1749762235.339)",
                expectedEndTime, callInfo.getEndTime(), 0.0001);

        // The critical bug: start time appears AFTER end time due to precision loss
        assertTrue("Start time should be before end time",
                callInfo.getStartTime() < callInfo.getEndTime());

        // Test more edge cases with leading zeros
        testLeadingZeroCase(1000000001L); // 001 milliseconds
        testLeadingZeroCase(1000000010L); // 010 milliseconds
        testLeadingZeroCase(1000000100L); // 100 milliseconds (should work)
    }

    private void testLeadingZeroCase(long millisTimestamp) {
        CallInfo callInfo = new CallInfo("provider", "model", millisTimestamp, millisTimestamp + 100, Map.of());
        double expected = millisTimestamp / 1000.0;
        double actual = callInfo.getStartTime();

        assertEquals("Timestamp " + millisTimestamp + " should preserve precision",
                expected, actual, 0.0001);
    }

    @Test
    public void testInvalidCustomerFeedback() {
        withMockedClient((HttpClient mockedClient) -> {
            String completionId = UUID.randomUUID().toString();
            Map<String, Object> feedback = Map.of(
                    "helpful", "thumbsup",
                    "not_allowed", new Object()
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            FreeplayClientException exception = assertThrows(
                    FreeplayClientException.class,
                    () -> fpClient.customerFeedback().update(projectId, completionId, feedback).get()
            );
            assertEquals(
                    "Invalid value for key 'not_allowed': Value must be a string, number, or boolean.",
                    exception.getMessage()
            );
        });
    }

    @Test
    public void testHandlesUnauthorizedOnCreateTestRun() {
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
            mockGetPromptV2Async(
                    mockedClient, templateName, "prod", getChatPromptContentObjects(), anthropicLLMParameters, "anthropic_chat"
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // Calling get() and chaining calls
            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            FreeplayConfigurationException exception = assertThrows(
                    FreeplayConfigurationException.class,
                    () -> templatePrompt
                            .bind(new TemplatePrompt.BindRequest(variables))
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
    public void testHandlesUnauthorizedOnGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedGetPromptsV2Async(mockedClient);

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
    public void testHandlesUnauthorizedOnRecord() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedRecordAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));
            StubbedRecordFixtures fixtures = new StubbedRecordFixtures("anthropic", MODEL_CLAUDE_2, "anthropic_chat", "Some completion");

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.recordings().create(
                            new RecordInfo(
                                    projectId,
                                    fixtures.getAllMessages()
                            ).inputs(variables)
                                    .promptVersionInfo(fixtures.getPromptInfo())
                                    .callInfo(fixtures.getCallInfo())
                                    .responseInfo(fixtures.getResponseInfo())).get());
            assertEquals("Error making call [401]", exception.getCause().getMessage());
        });
    }

    @Test
    public void testOpenAIToolSchemaFormatting() {
        withMockedClient((HttpClient mockedClient) -> {
            List<ToolSchema> toolSchema = List.of(
                    new ToolSchema("get_weather", "Get the weather for a location", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "location", Map.of("type", "string"),
                                    "unit", Map.of("type", "string", "enum", List.of("celsius", "fahrenheit"))
                            ),
                            "required", List.of("location")
                    ))
            );

            TemplatePrompt templatePrompt = new TemplatePrompt(
                    new PromptInfo(promptTemplateId, promptTemplateVersionId, templateName, "prod",
                            openAILLMParameters, "openai", MODEL_GPT_35_TURBO, "openai_chat"),
                    getChatPromptContentObjects().stream()
                            .map(obj -> new ChatMessage(
                                    (String) ((Map<?, ?>) obj).get("role"),
                                    (String) ((Map<?, ?>) obj).get("content")
                            ))
                            .collect(toList()),
                    toolSchema
            );

            FormattedPrompt<List<ChatMessage>> formatted = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format("openai_chat");

            List<Map<String, Object>> expectedToolSchema = List.of(Map.of(
                    "function", toolSchema.get(0),
                    "type", "function"
            ));

            assertEquals(expectedToolSchema, formatted.getToolSchema());
        });
    }

    @Test
    public void testAnthropicToolSchemaFormatting() {
        withMockedClient((HttpClient mockedClient) -> {
            List<ToolSchema> toolSchema = List.of(
                    new ToolSchema("get_weather", "Get the weather for a location", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "location", Map.of("type", "string"),
                                    "unit", Map.of("type", "string", "enum", List.of("celsius", "fahrenheit"))
                            ),
                            "required", List.of("location")
                    ))
            );

            TemplatePrompt templatePrompt = new TemplatePrompt(
                    new PromptInfo(promptTemplateId, promptTemplateVersionId, templateName, "prod",
                            anthropicLLMParameters, "anthropic", MODEL_CLAUDE_2, "anthropic_chat"),
                    getChatPromptContentObjects().stream()
                            .map(obj -> new ChatMessage(
                                    (String) ((Map<?, ?>) obj).get("role"),
                                    (String) ((Map<?, ?>) obj).get("content")
                            ))
                            .collect(toList()),
                    toolSchema
            );

            FormattedPrompt<List<ChatMessage>> formatted = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format("anthropic_chat");

            List<Map<String, Object>> expectedToolSchema = List.of(Map.of(
                    "name", "get_weather",
                    "description", "Get the weather for a location",
                    "input_schema", toolSchema.get(0).getParameters()
            ));

            assertEquals(expectedToolSchema, formatted.getToolSchema());
        });
    }

    @Test
    public void testFormattingWithoutToolSchema() {
        withMockedClient((HttpClient mockedClient) -> {
            // Create template prompt with null tool schema
            TemplatePrompt templatePrompt = new TemplatePrompt(
                    new PromptInfo(promptTemplateId, promptTemplateVersionId, templateName, "prod",
                            openAILLMParameters, "openai", MODEL_GPT_35_TURBO, "openai_chat"),
                    getChatPromptContentObjects().stream()
                            .map(obj -> new ChatMessage(
                                    (String) ((Map<?, ?>) obj).get("role"),
                                    (String) ((Map<?, ?>) obj).get("content")
                            ))
                            .collect(toList()),
                    null
            );

            FormattedPrompt<List<ChatMessage>> formatted = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format("openai_chat");

            // Tool schema should be null when not provided
            assertNull(formatted.getToolSchema());

            // Create template prompt with empty tool schema list
            TemplatePrompt templatePromptEmpty = new TemplatePrompt(
                    new PromptInfo(promptTemplateId, promptTemplateVersionId, templateName, "prod",
                            openAILLMParameters, "openai", MODEL_GPT_35_TURBO, "openai_chat"),
                    getChatPromptContentObjects().stream()
                            .map(obj -> new ChatMessage(
                                    (String) ((Map<?, ?>) obj).get("role"),
                                    (String) ((Map<?, ?>) obj).get("content")
                            ))
                            .collect(toList()),
                    List.of()
            );

            FormattedPrompt<List<ChatMessage>> formattedEmpty = templatePromptEmpty
                    .bind(new TemplatePrompt.BindRequest(variables))
                    .format("openai_chat");

            // Tool schema should be empty list when empty list provided
            List<Map<String, Object>> expectedEmptySchema = List.of();
            assertEquals(expectedEmptySchema, formattedEmpty.getToolSchema());
        });
    }

    @Test
    public void testTestRunCreatedWithFlavorName() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateTestRunAsync(mockedClient);

            String testListName = "core-tests";
            String flavorName = "anthropic_chat";
            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TestRunRequest testRunRequest = fpClient.testRuns()
                    .createRequest(projectId, testListName)
                    .flavorName(flavorName)
                    .includeOutputs(true)
                    .build();

            TestRun testRun = fpClient.testRuns().create(testRunRequest).get();

            assertEquals(2, testRun.getTestCases().size());
            assertNotNull(testRun.getTestCases().get(0).getTestCaseId());
            assertEquals("Why isn't my sink working?", testRun.getTestCases().get(0).getVariables().get("question"));
            assertEquals("It took PTO today", testRun.getTestCases().get(0).getOutput());
            assertNotNull(testRun.getTestCases().get(1).getTestCaseId());
            assertEquals("Why isn't my internet working?", testRun.getTestCases().get(1).getVariables().get("question"));
            assertEquals("It's playing golf with the sink", testRun.getTestCases().get(1).getOutput());

            String requestBody = getCapturedAsyncBody(mockedClient, 1, 0);
            assertTrue(requestBody.contains("\"flavor_name\":\"" + flavorName + "\""));
        });
    }

    private class StubbedRecordFixtures {
        private final BoundPrompt boundPrompt;
        private final String completion;

        private final PromptInfo promptInfo;
        private final CallInfo callInfo;
        private final ResponseInfo responseInfo = new ResponseInfo(true);
        private final Map<String, Object> modelParameters;
        private final Map<String, Object> providerInfo;
        private final List<Map<String, Object>> toolSchema;

        public StubbedRecordFixtures(
                String provider,
                String model,
                String flavorName,
                String completion
        ) {
            modelParameters = Map.of("max_tokens", 256);
            providerInfo = Map.of("provider_info_key", "provider_info_value");
            promptInfo = new PromptInfo(
                    promptTemplateId,
                    promptTemplateVersionId,
                    templateName,
                    "prod",
                    modelParameters,
                    provider,
                    model,
                    flavorName
            ).providerInfo(providerInfo);
            this.completion = completion;

            callInfo = new CallInfo(
                    provider,
                    model,
                    System.currentTimeMillis() - 10,
                    System.currentTimeMillis(),
                    modelParameters
            ).providerInfo(providerInfo);

            boundPrompt = new TemplatePrompt(
                    getPromptInfo(),
                    List.of(
                            new ChatMessage("system", "You are a support agent."),
                            new ChatMessage("assistant", "How may I help you?"),
                            new ChatMessage("user", "{{question}}")
                    )).bind(new TemplatePrompt.BindRequest(variables));
            toolSchema = List.of(
                    Map.of(
                            "name", "get_weather",
                            "description", "Get the weather for a location",
                            "input_schema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "location", Map.of("type", "string"),
                                            "unit", Map.of("type", "string", "enum", List.of("celsius", "fahrenheit"))
                                    ),
                                    "required", List.of("location")
                            )
                    )
            );
        }

        @SuppressWarnings("unused")
        public String getCompletion() {
            return completion;
        }

        @SuppressWarnings("unused")
        public Map<String, Object> getModelParameters() {
            return modelParameters;
        }

        @SuppressWarnings("unused")
        public Map<String, Object> getProviderInfo() {
            return providerInfo;
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

        public List<Map<String, Object>> getToolSchema() {
            return toolSchema;
        }

        public List<ChatMessage> getAllMessages() {
            return getBoundPrompt()
                    .format(promptInfo.getFlavorName())
                    .allMessages(new ChatMessage("Assistant", completion));
        }
    }

    @Test
    public void testRecordInfoParentId() {
        String projectId = "test-project";
        List<ChatMessage> allMessages = List.of(
            new ChatMessage("user", "test message"),
            new ChatMessage("assistant", "test response")
        );
        
        UUID parentId = UUID.randomUUID();
        
        RecordInfo recordInfo = new RecordInfo(projectId, allMessages)
                .parentId(parentId);
        
        assertEquals(parentId, recordInfo.getParentId());
    }

    @Test
    public void testTraceInfoParentId() {
        ThinCallSupport mockCallSupport = Mockito.mock(ThinCallSupport.class);
        UUID sessionId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        String input = "test input";
        
        TraceInfo traceInfo = new TraceInfo(sessionId, traceId, input, mockCallSupport)
                .parentId(parentId);  // Using builder pattern
        
        assertEquals(sessionId, traceInfo.getSessionId());
        assertEquals(traceId, traceInfo.getTraceId());
        assertEquals(input, traceInfo.getInput());
        assertEquals(parentId, traceInfo.getParentId());
    }

    @Test
    public void testTraceInfoWithoutParentId() {
        ThinCallSupport mockCallSupport = Mockito.mock(ThinCallSupport.class);
        UUID sessionId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        String input = "test input";
        
        TraceInfo traceInfo = new TraceInfo(sessionId, traceId, input, mockCallSupport);
        
        assertEquals(sessionId, traceInfo.getSessionId());
        assertEquals(traceId, traceInfo.getTraceId());
        assertEquals(input, traceInfo.getInput());
        assertNull(traceInfo.getParentId());  // Should be null when not specified
    }

    @Test
    public void testSessionCreateTraceWithParentId() {
        ThinCallSupport mockCallSupport = Mockito.mock(ThinCallSupport.class);
        Session session = new Session(mockCallSupport);
        
        String input = "parent question";
        String agentName = "test_agent";
        Map<String, Object> metadata = Map.of("level", "parent");
        
        TraceInfo parentTrace = session.createTrace(input).agentName(agentName).customMetadata(metadata);
        assertNull(parentTrace.getParentId());  // Parent trace has no parent
        
        // Create child trace with parent ID
        String childInput = "child question";
        String childAgentName = "child_agent";
        Map<String, Object> childMetadata = Map.of("level", "child");
        UUID parentId = parentTrace.getTraceId();
        
        TraceInfo childTrace = session.createTrace(childInput).agentName(childAgentName).customMetadata(childMetadata).parentId(parentId);
        
        assertEquals(childInput, childTrace.getInput());
        assertEquals(childAgentName, childTrace.getAgentName());
        assertEquals(parentId, childTrace.getParentId());
        assertEquals(childMetadata, childTrace.getCustomMetadata());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedTraceInfoMethodsStillWork() {
        String projectId = "test-project";
        List<ChatMessage> allMessages = List.of(
            new ChatMessage("user", "test message"),
            new ChatMessage("assistant", "test response")
        );
        
        ThinCallSupport mockCallSupport = Mockito.mock(ThinCallSupport.class);
        UUID sessionId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        String input = "test input";
        
        TraceInfo traceInfo = new TraceInfo(sessionId, traceId, input, mockCallSupport);
        
        RecordInfo recordInfo = new RecordInfo(projectId, allMessages)
                .traceInfo(traceInfo);
        
        assertEquals(traceInfo, recordInfo.getTraceInfo());  // Using deprecated getter
    }

    @Test
    public void testParentIdVsTraceInfoEquivalence() {
        String projectId = "test-project";
        List<ChatMessage> allMessages = List.of(
            new ChatMessage("user", "test message"),
            new ChatMessage("assistant", "test response")
        );
        
        ThinCallSupport mockCallSupport = Mockito.mock(ThinCallSupport.class);
        UUID sessionId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        String input = "test input";
        
        TraceInfo traceInfo = new TraceInfo(sessionId, traceId, input, mockCallSupport);
        
        // Create with deprecated traceInfo
        @SuppressWarnings("deprecation")
        RecordInfo recordWithTraceInfo = new RecordInfo(projectId, allMessages)
                .traceInfo(traceInfo);
        
        // Create with new parentId approach
        RecordInfo recordWithParentId = new RecordInfo(projectId, allMessages)
                .parentId(traceInfo.getTraceId());
        
        // Both should reference the same trace ID
        @SuppressWarnings("deprecation")
        UUID traceInfoId = recordWithTraceInfo.getTraceInfo().getTraceId();
        UUID parentIdValue = recordWithParentId.getParentId();
        
        assertEquals(traceInfoId, parentIdValue);
    }

    @Test
    public void testMultiLevelTraceHierarchy() {
        ThinCallSupport mockCallSupport = Mockito.mock(ThinCallSupport.class);
        Session session = new Session(mockCallSupport);
        
        // Create root trace
        TraceInfo root = session.createTrace("root question").agentName("root_agent").customMetadata(null);
        assertNull(root.getParentId());
        
        // Create child trace
        TraceInfo child = session.createTrace("child question").agentName("child_agent").customMetadata(null).parentId(root.getTraceId());
        assertEquals(root.getTraceId(), child.getParentId());
        
        // Create grandchild trace
        TraceInfo grandchild = session.createTrace("grandchild question").agentName("grandchild_agent").customMetadata(null).parentId(child.getTraceId());
        assertEquals(child.getTraceId(), grandchild.getParentId());
        
        // Verify hierarchy
        assertNull(root.getParentId());
        assertEquals(root.getTraceId(), child.getParentId());
        assertEquals(child.getTraceId(), grandchild.getParentId());
        
        // All IDs should be unique
        assertNotEquals(root.getTraceId(), child.getTraceId());
        assertNotEquals(child.getTraceId(), grandchild.getTraceId());
        assertNotEquals(root.getTraceId(), grandchild.getTraceId());
    }
}
