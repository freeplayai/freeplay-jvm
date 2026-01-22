package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.ChatStart;
import ai.freeplay.client.model.IndexedChatMessage;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.RecordProcessor.DO_NOT_RECORD_PROCESSOR;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class OpenAIContinuousChatTest extends HttpClientTestBase {

    private final String templateName = "my-chat-start";

    @Test
    public void chatStartsAndContinues() {
        String completion1 = "\\n\\nSorry, I will try to help";
        String completion2 = "\\n\\nMa dai, hai anche provato a controllare se l'acqua è aperta? Magari è per quello";
        String completion1Expected = "\n\nSorry, I will try to help";
        String completion2Expected = "\n\nMa dai, hai anche provato a controllare se l'acqua è aperta? Magari è per quello";
        String formattedPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";
        String formattedPrompt2Expected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"},{\"content\":\"\\n\\nSorry, I will try to help\",\"role\":\"assistant\"},{\"content\":\"Now in Italian!\",\"role\":\"user\"}]";

        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mock2OpenAICalls(mockedClient, completion1, completion2);
            mockRecord(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );

            // Start
            // --------
            ChatStart<IndexedChatMessage> chatStart = fpClient.startChat(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    "latest",
                    null,
                    Map.of("customer_id", "abc", "true", false)
            );

            // Completion
            assertEquals(completion1Expected, chatStart.getFirstCompletion().getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));

            assertEquals(formattedPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(completion1Expected, recordBodyMap.get("return_content"));
            assertEquals(MODEL_GPT_35_TURBO, recordBodyMap.get("model"));
            assertEquals(Map.of("customer_id", "abc", "true", false), recordBodyMap.get("custom_metadata"));
            assertNull(recordBodyMap.get("test_run_id"));

            // Continue
            // --------
            chatStart.getSession().continueChat(new ChatMessage("user", "Now in Italian!"));

            // Completion
            assertEquals(6, chatStart.getSession().getMessageHistory().size());
            //noinspection OptionalGetWithoutIsPresent
            assertEquals(completion2Expected, chatStart.getSession().getLastMessage().get().getContent());

            // Record call
            Map<String, Object> record2BodyMap = getCapturedBodyAsMap(mockedClient, 5, 4);
            assertEquals(promptTemplateVersionId, record2BodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, record2BodyMap.get("prompt_template_id"));
            assertEquals(formattedPrompt2Expected, record2BodyMap.get("prompt_content"));
            assertEquals(completion2Expected, record2BodyMap.get("return_content"));
            assertEquals(MODEL_GPT_35_TURBO, record2BodyMap.get("model"));
            assertNull(record2BodyMap.get("test_run_id"));
        });
    }

    @Test
    public void disallowsMessagesParam() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.startChat(
                        projectId,
                        templateName,
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_GPT_35_TURBO,
                                "messages", Map.of("this is", "not allowed")
                        ),
                        "latest"
                );
                fail("Should have gotten an exception disallowing the prompt parameter");
            } catch (FreeplayException fpe) {
                assertEquals(
                        "The 'messages' parameter cannot be specified. It is populated automatically.",
                        fpe.getMessage());
            }
        });
    }

    @Test
    public void handlesUnauthorizedCallingOpenAI() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockUnauthorizedOpenAIChatCall(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
                fpClient.startChat(
                        projectId,
                        templateName,
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
            mockOpenAIChatCalls(mockedClient, "\\n\\nSorry, I will try to help");


            Map<String, Object> clientParams = Map.of("max_tokens", "33");
            Map<String, Object> callParams = Map.of("temperature", "0.44");

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey), clientParams);
            fpClient.startChat(
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
        });
    }

    @Test
    public void chatDoesNotRecordWhenAskedNotTo() {
        String completion1 = "\\n\\nSorry, I will try to help";
        String completion1Expected = "\n\nSorry, I will try to help";

        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCalls(mockedClient, completion1);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey)),
                    DO_NOT_RECORD_PROCESSOR);

            // Start
            // --------
            ChatStart<IndexedChatMessage> chatStart = fpClient.startChat(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    "latest"
            );

            // Completion
            assertEquals(completion1Expected, chatStart.getFirstCompletion().getContent());

            // Record call
            assertTrue(routeNotCalled(mockedClient, 2, "record"));
        });
    }


    @SuppressWarnings("SameParameterValue")
    private void mock2OpenAICalls(HttpClient mockedClient, String completion1, String completion2) throws RuntimeException {
        try {
            when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                    .thenReturn(response(200, getOpenAIChatResponse(completion1)))
                    .thenReturn(response(200, getOpenAIChatResponse(completion2)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
