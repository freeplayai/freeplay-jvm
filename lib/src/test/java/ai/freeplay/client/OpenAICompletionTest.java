package ai.freeplay.client;

import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.*;
import ai.freeplay.client.processor.LLMCallInfo;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static ai.freeplay.client.CompletionFeedback.NEGATIVE_FEEDBACK;
import static ai.freeplay.client.RecordProcessor.DO_NOT_RECORD_PROCESSOR;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.MockMethods.routeNotCalled;
import static ai.freeplay.client.internal.utilities.PromptProcessors.testChatProcessor;
import static org.junit.Assert.*;

public class OpenAICompletionTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";

    private final String chatCompletion1 = "\\n\\nSorry, I will try to help";
    private final String chatCompletion1Expected = "\n\nSorry, I will try to help";
    private final String formattedChatPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";
    private final String chatPromptWithInsertedMessage = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"Inserted Message\",\"role\":\"user\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";

    private final ProviderConfigs providerConfigs = new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey));

    // Chat prompt templates can be called through getCompletion and getChatCompletion,
    // the latter to get access to chat-specific fields. Test both.
    @Test
    public void chatReturnsFromCompletionCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);

            CompletionResponse completionResponse = fpClient.getCompletion(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    "latest"
            );

            // Completion
            assertEquals(chatCompletion1Expected, completionResponse.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(formattedChatPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(chatCompletion1Expected, recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void chatReturnsFromChatCompletionCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);

            CompletionSession session = fpClient.createSession(projectId, "latest");
            ChatCompletionResponse completionResponse = session.getChatCompletion(
                    templateName,
                    Map.of("question", "why isn't my sink working?")
            );

            // Completion
            assertEquals(chatCompletion1Expected, completionResponse.getContent());
            assertTrue(completionResponse.getFirstChoice().isPresent());
            assertEquals(chatCompletion1Expected, completionResponse.getFirstChoice().get().getContent());
            assertEquals("assistant", completionResponse.getFirstChoice().get().getRole());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(formattedChatPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(chatCompletion1Expected, recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void chainRecordsCorrectly() {
        String completion1 = "\\n\\nSorry, I will try to help";
        String formattedPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";
        String formattedPrompt2Expected = "[{\"content\":\"You are a movie reviewer\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"Summarize this move: War Games\",\"role\":\"user\"}]";

        String template2Name = "movie-summarizer";
        String template2Content = JSONUtil.asString(array(
                object(
                        "role", "system",
                        "content", "You are a movie reviewer"
                ),
                object(
                        "role", "assistant",
                        "content", "How may I help you?"
                ),
                object(
                        "role", "user",
                        "content", "Summarize this move: {{movie}}"
                )
        ));
        String completion2 = "\\n\\nA young hacker...";

        PromptTemplate[] promptFixtures = {
                new PromptTemplate(
                        templateName,
                        getChatPromptContent(),
                        "openai_chat",
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        Map.of("model", MODEL_GPT_35_TURBO)),
                new PromptTemplate(
                        template2Name,
                        template2Content,
                        "openai_chat",
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        Map.of("model", MODEL_GPT_35_TURBO))};

        withMockedClient((HttpClient mockedClient) -> {
            mockGet2PromptsV2(mockedClient, promptFixtures);
            mockOpenAIChatCalls(mockedClient, completion1, completion2);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);
            CompletionSession session = fpClient.createSession(projectId, "latest");
            ChatCompletionResponse response1 = session.getChatCompletion(
                    templateName,
                    Map.of("question", "why isn't my sink working?")
            );

            // First call
            assertTrue(response1.getFirstChoice().isPresent());
            assertEquals(unescapeExpected(completion1), response1.getContent());
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(session.getSessionId(), recordBodyMap.get("session_id"));
            assertEquals(formattedPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(unescapeExpected(completion1), recordBodyMap.get("return_content"));

            // Second Call
            CompletionResponse response2 = session.getCompletion(
                    template2Name,
                    Map.of("movie", "War Games"));

            assertEquals(unescapeExpected(completion2), response2.getContent());
            Map<String, Object> record2BodyMap = getCapturedBodyAsMap(mockedClient, 5, 4);
            assertEquals(session.getSessionId(), record2BodyMap.get("session_id"));
            assertEquals(formattedPrompt2Expected, record2BodyMap.get("prompt_content"));
            assertEquals(unescapeExpected(completion2), record2BodyMap.get("return_content"));
        });
    }

    @Test
    public void disallowsPromptParam() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_GPT_35_TURBO,
                                "prompt", "this is not allowed"
                        ),
                        "latest"
                );
                fail("Should have gotten an exception disallowing the prompt parameter");
            } catch (FreeplayException fpe) {
                assertEquals(
                        "The 'prompt' parameter cannot be specified. It is populated automatically.",
                        fpe.getMessage());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chatCompletionHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);

            AtomicReference<LLMCallInfo> llmCallInfo = new AtomicReference<>();

            fpClient.getCompletion(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    "latest",
                    (Collection<ChatMessage> messages, LLMCallInfo info) -> {
                        llmCallInfo.set(info);
                        List<ChatMessage> newMessages = new ArrayList<>(messages);
                        newMessages.add(1, new ChatMessage("user", "Inserted Message"));
                        return newMessages;
                    }
            );

            // Modified OpenAI call
            Map<String, Object> openAiRequestBody = getCapturedBodyAsMap(mockedClient, 3, 1);
            assertEquals(4, asList(openAiRequestBody.get("messages")).size());
            Map<String, Object> inserted = (Map<String, Object>) asList(openAiRequestBody.get("messages")).get(1);
            assertEquals("user", inserted.get("role"));
            assertEquals("Inserted Message", inserted.get("content"));
            assertEquals("gpt-3.5-turbo", llmCallInfo.get().getLLMParameters().get("model"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(chatPromptWithInsertedMessage, recordBodyMap.get("prompt_content"));
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chatCompletionAsChatHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);
            mockRecord(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);
            CompletionSession session = fpClient.createSession(projectId, "latest");
            session.getChatCompletion(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    testChatProcessor
            );

            // Modified OpenAI call
            Map<String, Object> openAiRequestBody = getCapturedBodyAsMap(mockedClient, 3, 1);
            assertEquals(4, asList(openAiRequestBody.get("messages")).size());
            Map<String, Object> inserted = (Map<String, Object>) asList(openAiRequestBody.get("messages")).get(1);
            assertEquals("user", inserted.get("role"));
            assertEquals("Inserted Message", inserted.get("content"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(chatPromptWithInsertedMessage, recordBodyMap.get("prompt_content"));
        });
    }

    @Test
    public void handlesUnauthorizedOnGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedGetPrompts(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        "latest"
                );
                fail("Should have gotten an exception for a 401");
            } catch (FreeplayClientException fpe) {
                assertEquals("Error making call [401]", fpe.getMessage());
            }
        });
    }

    @Test
    public void handlesUnauthorizedCallingOpenAI() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockUnauthorizedOpenAIChatCall(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of("model", MODEL_GPT_35_TURBO),
                        "latest"
                );
                fail("Should have gotten an exception for a 401");
            } catch (FreeplayException fpe) {
                assertEquals("Error making call [401]", fpe.getMessage());
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlesLLMParameterMergePrecedence() {
        withMockedClient((HttpClient mockedClient) -> {
                    mockGetPromptsV2(
                            mockedClient,
                            templateName,
                            getChatPromptContentObjects(),
                            Map.of(
                                    "model", "gpt-3.5-turbo",
                                    "max_tokens", "11",
                                    "temperature", "0.22"
                            ),
                            "openai_chat"
                    );
                    mockOpenAIChatCalls(mockedClient, chatCompletion1);

                    Map<String, Object> clientParams = Map.of("max_tokens", "33");
                    Map<String, Object> callParams = Map.of("temperature", "0.44");

                    Freeplay fpClient = new Freeplay(
                            freeplayApiKey, baseUrl, providerConfigs, clientParams);

                    fpClient.getCompletion(
                            projectId,
                            templateName,
                            Map.of("question", "why isn't my sink working?"),
                            callParams,
                            "latest"
                    );

                    Map<String, Object> openaiBody = getCapturedBodyAsMap(mockedClient, 3, 1);
                    assertEquals("gpt-3.5-turbo", openaiBody.get("model"));
                    assertEquals("33", openaiBody.get("max_tokens"));
                    assertEquals("0.44", openaiBody.get("temperature"));

                    Map<String, Object> recordBody = getCapturedBodyAsMap(mockedClient, 3, 2);
                    Map<String, Object> recordedParameters = (Map<String, Object>) recordBody.get("llm_parameters");
                    assertEquals("gpt-3.5-turbo", recordedParameters.get("model"));
                    assertEquals("33", recordedParameters.get("max_tokens"));
                    assertEquals("0.44", recordedParameters.get("temperature"));
                }
        );
    }

    @Test
    public void unconfiguredProviderErrors() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new ProviderConfigs(new AnthropicProviderConfig("")));

            try {
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of("model", MODEL_GPT_35_TURBO),
                        "latest"
                );
            } catch (FreeplayException fpe) {
                assertEquals("The OpenAI provider is not configured on the ProviderConfig. " +
                        "Set up this provider config to call OpenAI endpoints.", fpe.getMessage());
            }
        });
    }

    @Test
    public void chatDoesNotRecordWhenAskedNotTo() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    providerConfigs,
                    DO_NOT_RECORD_PROCESSOR);

            CompletionResponse completionResponse = fpClient.getCompletion(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    "latest"
            );

            // Completion
            assertEquals(chatCompletion1Expected, completionResponse.getContent());

            // Make sure the Record call didn't happen (it would be the 3rd)
            assertTrue(routeNotCalled(mockedClient, 2, "record"));
        });
    }

    @Test
    public void chatCompletionFeedback() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, chatCompletion1);
            mockRecord(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, providerConfigs);

            CompletionResponse completionResponse = fpClient.getCompletion(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    "latest"
            );

            String completionId = completionResponse.getCompletionId();
            fpClient.recordCompletionFeedback(projectId, completionId, Map.of("feedback", NEGATIVE_FEEDBACK));

            // Make sure we record feedback
            Map<String, Object> feedbackMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(NEGATIVE_FEEDBACK, feedbackMap.get("feedback"));
        });
    }
}
