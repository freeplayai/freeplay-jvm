package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.PromptTemplate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class OpenAICompletionTest {

    private final String templateName = "my-prompt";

    private final String textPromptContent = "Answer this question: {{question}}";
    private final String textCompletion = "\\n\\nI apologize that your sink isn't working. Can I help you";

    private final String chatCompletion1 = "\\n\\nSorry, I will try to help";
    private final String chatCompletion1Expected = "\n\nSorry, I will try to help";
    private final String formattedChatPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";

    private HttpClient mockedClient;

    @Before
    public void beforeEach() {
        mockedClient = mock(HttpClient.class);
    }

    @Test
    public void textCompletionReturnsValue() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");
        mockOpenAITextCall(mockedClient, textCompletion);

        CompletionResponse completion;

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            completion = fpClient.getCompletion(
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
        }
    }

    // Chat prompt templates can be called through getCompletion and getChatCompletion,
    // the latter to get access to chat-specific fields. Test both.
    @Test
    public void chatReturnsFromCompletionCall() throws Exception {
        CompletionResponse completionResponse;

        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
        mockOpenAIChatCall(mockedClient, chatCompletion1);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));

            completionResponse = fpClient.getCompletion(
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
        }
    }

    @Test
    public void chatReturnsFromChatCompletionCall() throws Exception {
        ChatCompletionResponse completionResponse;

        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
        mockOpenAIChatCall(mockedClient, chatCompletion1);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));

            CompletionSession session = fpClient.createSession(projectId, "latest");
            completionResponse = session.getChatCompletion(
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
        }
    }

    @Test
    public void chainRecordsCorrectly() throws Exception {
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

        mockCreateSession(mockedClient);
        mockGet2Prompts(mockedClient, promptFixtures);
        mockOpenAIChatCall(mockedClient, completion1);
        mockOpenAITextCall(mockedClient, completion2);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
        }
    }

    @Test
    public void requiresModelParam() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
    }

    @Test
    public void disallowsPromptParam() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
    }

    @Test
    public void handlesUnauthorizedOnCreateSession() throws Exception {
        mockUnauthorizedCreateSession(mockedClient);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
    }

    @Test
    public void handlesUnauthorizedOnGetPrompts() throws Exception {
        mockCreateSession(mockedClient);
        mockUnauthorizedGetPrompts(mockedClient);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
    }

    @Test
    public void handlesUnauthorizedCallingOpenAI() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "openai_text");
        mockUnauthorizedOpenAITextCall(mockedClient);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlesLLMParameterMergePrecedence() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(
                mockedClient,
                templateName,
                textPromptContent,
                Map.of(
                        "model", "gpt-turbo-3.5",
                        "max_tokens", "11",
                        "temperature", "0.22"
                ), "openai_text"
        );
        mockOpenAITextCall(mockedClient, textCompletion);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

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
            assertEquals("gpt-turbo-3.5", openaiBody.get("model"));
            assertEquals("33", openaiBody.get("max_tokens"));
            assertEquals("0.44", openaiBody.get("temperature"));

            Map<String, Object> recordBody = getCapturedBodyAsMap(mockedClient, 4, 3);
            Map<String, Object> recordedParameters = (Map<String, Object>) recordBody.get("llm_parameters");
            assertEquals("gpt-turbo-3.5", recordedParameters.get("model"));
            assertEquals("33", recordedParameters.get("max_tokens"));
            assertEquals("0.44", recordedParameters.get("temperature"));
        }
    }
}
