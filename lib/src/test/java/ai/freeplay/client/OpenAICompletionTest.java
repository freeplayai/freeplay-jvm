package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.*;
import ai.freeplay.client.processor.LLMCallInfo;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.PromptProcessors.testChatProcessor;
import static org.junit.Assert.*;

public class OpenAICompletionTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";

    private final String textPromptContent = "Answer this question: {{question}}";
    private final String textCompletion = "\\n\\nI apologize that your sink isn't working. Can I help you";

    private final String chatCompletion1 = "\\n\\nSorry, I will try to help";
    private final String chatCompletion1Expected = "\n\nSorry, I will try to help";
    private final String formattedChatPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";
    private final String chatPromptWithInsertedMessage = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"Inserted Message\",\"role\":\"user\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";

    @Test
    public void textCompletionReturnsValue() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");
            mockOpenAITextCall(mockedClient, textCompletion);


            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            CompletionResponse completion = fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of("model", MODEL_TEXT_DAVINCI_003),
                    "latest"
            );

            // Completion
            String textCompletionExpected = "\n\nI apologize that your sink isn't working. Can I help you";
            assertEquals(textCompletionExpected, completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));
            assertEquals("Answer this question: why isn't my sink working?", recordBodyMap.get("prompt_content"));
            assertEquals("\n\nI apologize that your sink isn't working. Can I help you", recordBodyMap.get("return_content"));
            assertNull(recordBodyMap.get("test_run_id"));
        });
    }

    // Chat prompt templates can be called through getCompletion and getChatCompletion,
    // the latter to get access to chat-specific fields. Test both.
    @Test
    public void chatReturnsFromCompletionCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
            mockOpenAIChatCall(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));

            CompletionResponse completionResponse = fpClient.getCompletion(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    "latest"
            );

            // Completion
            assertEquals(chatCompletion1Expected, completionResponse.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(formattedChatPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(chatCompletion1Expected, recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void chatReturnsFromChatCompletionCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
            mockOpenAIChatCall(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));

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
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(formattedChatPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(chatCompletion1Expected, recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void chainRecordsCorrectly() {
        String completion1 = "\\n\\nSorry, I will try to help";
        String formattedPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";

        String template2Name = "movie-summarizer";
        String prompt2Content = "Summarize the plot of this movie: {{movie}}";
        String completion2 = "\\n\\nA young hacker...";
        String formattedPrompt2Expected = "Summarize the plot of this movie: War Games";

        PromptTemplate[] promptFixtures = {
                new PromptTemplate(
                        templateName,
                        getChatPromptContent(),
                        "openai_chat",
                        UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        Map.of("model", MODEL_GPT_TURBO_35)),
                new PromptTemplate(
                        template2Name,
                        prompt2Content,
                        "openai_text",
                        UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        Map.of("model", MODEL_TEXT_DAVINCI_003))};

        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGet2Prompts(mockedClient, promptFixtures);
            mockOpenAIChatCall(mockedClient, completion1);
            mockOpenAITextCall(mockedClient, completion2);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            CompletionSession session = fpClient.createSession(projectId, "latest");
            ChatCompletionResponse response1 = session.getChatCompletion(
                    templateName,
                    Map.of("question", "why isn't my sink working?")
            );

            // First call
            assertTrue(response1.getFirstChoice().isPresent());
            assertEquals(unescapeExpected(completion1), response1.getContent());
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(session.getSessionId(), recordBodyMap.get("session_id"));
            assertEquals(formattedPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(unescapeExpected(completion1), recordBodyMap.get("return_content"));

            // Second Call
            CompletionResponse response2 = session.getCompletion(
                    template2Name,
                    Map.of("movie", "War Games"));

            assertEquals(unescapeExpected(completion2), response2.getContent());
            Map<String, Object> record2BodyMap = getCapturedBodyAsMap(mockedClient, 6, 5);
            assertEquals(session.getSessionId(), record2BodyMap.get("session_id"));
            assertEquals(formattedPrompt2Expected, record2BodyMap.get("prompt_content"));
            assertEquals(unescapeExpected(completion2), record2BodyMap.get("return_content"));
        });
    }

    @Test
    public void requiresModelParam() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");

            try {

                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Collections.emptyMap(),
                        "latest"
                );
                fail("Should have gotten an exception requiring the model parameter");
            } catch (FreeplayException fpe) {
                assertEquals("The 'model' parameter is required when calling OpenAI", fpe.getMessage());
            }
        });
    }

    @Test
    public void disallowsPromptParam() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_TEXT_DAVINCI_003,
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

    @Test
    public void textCompletionHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");
            mockOpenAITextCall(mockedClient, textCompletion);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));

            AtomicReference<LLMCallInfo> llmCallInfo = new AtomicReference<>();

            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of("model", MODEL_TEXT_DAVINCI_003),
                    "latest",
                    (String prompt, LLMCallInfo info) -> {
                        llmCallInfo.set(info);
                        return "PREPENDED_TEXT " + prompt;
                    }
            );

            // Modified OpenAI call
            Map<String, Object> openAiRequestBody = getCapturedBodyAsMap(mockedClient, 4, 2);
            assertEquals(
                    "PREPENDED_TEXT Answer this question: why isn't my sink working?",
                    openAiRequestBody.get("prompt"));
            assertEquals("text-davinci-003", llmCallInfo.get().getLLMParameters().get("model"));
            assertEquals(Provider.OpenAI, llmCallInfo.get().getProvider());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(
                    "PREPENDED_TEXT Answer this question: why isn't my sink working?",
                    recordBodyMap.get("prompt_content"));
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chatCompletionHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
            mockOpenAIChatCall(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));

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
            Map<String, Object> openAiRequestBody = getCapturedBodyAsMap(mockedClient, 4, 2);
            assertEquals(4, asList(openAiRequestBody.get("messages")).size());
            Map<String, Object> inserted = (Map<String, Object>) asList(openAiRequestBody.get("messages")).get(1);
            assertEquals("user", inserted.get("role"));
            assertEquals("Inserted Message", inserted.get("content"));
            assertEquals("gpt-3.5-turbo", llmCallInfo.get().getLLMParameters().get("model"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(chatPromptWithInsertedMessage, recordBodyMap.get("prompt_content"));
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chatCompletionAsChatHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
            mockOpenAIChatCall(mockedClient, chatCompletion1);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            CompletionSession session = fpClient.createSession(projectId, "latest");
            session.getChatCompletion(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    testChatProcessor
            );

            // Modified OpenAI call
            Map<String, Object> openAiRequestBody = getCapturedBodyAsMap(mockedClient, 4, 2);
            assertEquals(4, asList(openAiRequestBody.get("messages")).size());
            Map<String, Object> inserted = (Map<String, Object>) asList(openAiRequestBody.get("messages")).get(1);
            assertEquals("user", inserted.get("role"));
            assertEquals("Inserted Message", inserted.get("content"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(chatPromptWithInsertedMessage, recordBodyMap.get("prompt_content"));
        });
    }

    @Test
    public void handlesUnauthorizedOnCreateSession() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedCreateSession(mockedClient);

            try {

                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        "latest"
                );
                fail("Should have gotten an exception for a 401");
            } catch (FreeplayException fpe) {
                assertEquals("Error calling Freeplay [401]", fpe.getMessage());
            }
        });
    }

    @Test
    public void handlesUnauthorizedOnGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockUnauthorizedGetPrompts(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        "latest"
                );
                fail("Should have gotten an exception for a 401");
            } catch (FreeplayException fpe) {
                assertEquals("Error calling Freeplay [401]", fpe.getMessage());
            }
        });
    }

    @Test
    public void handlesUnauthorizedCallingOpenAI() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");
            mockUnauthorizedOpenAITextCall(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of("model", MODEL_TEXT_DAVINCI_003),
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
                    mockCreateSession(mockedClient);
                    mockGetPrompts(
                            mockedClient,
                            templateName,
                            textPromptContent,
                            Map.of(
                                    "model", "gpt-3.5-turbo",
                                    "max_tokens", "11",
                                    "temperature", "0.22"
                            ), "openai_text"
                    );
                    mockOpenAITextCall(mockedClient, textCompletion);

                    Map<String, Object> clientParams = Map.of("max_tokens", "33");
                    Map<String, Object> callParams = Map.of("temperature", "0.44");

                    Freeplay fpClient = new Freeplay(
                            freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey), clientParams);

                    fpClient.getCompletion(
                            projectId,
                            templateName,
                            Map.of("question", "why isn't my sink working?"),
                            callParams,
                            "latest"
                    );

                    Map<String, Object> openaiBody = getCapturedBodyAsMap(mockedClient, 4, 2);
                    assertEquals("gpt-3.5-turbo", openaiBody.get("model"));
                    assertEquals("33", openaiBody.get("max_tokens"));
                    assertEquals("0.44", openaiBody.get("temperature"));

                    Map<String, Object> recordBody = getCapturedBodyAsMap(mockedClient, 4, 3);
                    Map<String, Object> recordedParameters = (Map<String, Object>) recordBody.get("llm_parameters");
                    assertEquals("gpt-3.5-turbo", recordedParameters.get("model"));
                    assertEquals("33", recordedParameters.get("max_tokens"));
                    assertEquals("0.44", recordedParameters.get("temperature"));
                }
        );
    }
}
