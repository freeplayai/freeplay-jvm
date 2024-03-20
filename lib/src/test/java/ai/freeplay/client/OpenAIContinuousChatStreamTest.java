package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.ChatStart;
import ai.freeplay.client.model.IndexedChatMessage;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.RecordProcessor.DO_NOT_RECORD_PROCESSOR;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class OpenAIContinuousChatStreamTest extends HttpClientTestBase {

    private final String templateName = "my-chat-start";

    @Test
    public void chatStartsAndContinues() {
        String completion1Expected = "Well hello";
        String completion2Expected = "Bene ciao";
        String formattedPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";
        String formattedPrompt2Expected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"},{\"content\":\"Well hello\",\"role\":\"assistant\"},{\"content\":\"Now in Italian!\",\"role\":\"user\"}]";
        Map<String, Object> customMetadata = Map.of("chat_type", "automated");

        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mock2OpenAIChatStreamCalls(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );
            Map<String, Object> llmParameters = Collections.emptyMap();

            // Start
            ChatStart<Stream<IndexedChatMessage>> chatStart = fpClient.startChatStream(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    llmParameters,
                    "latest",
                    null,
                    customMetadata
            );

            List<IndexedChatMessage> startChunks = chatStart.getFirstCompletion().collect(Collectors.toList());

            // Completion
            assertEquals(4, startChunks.size());
            assertEquals("assistant", startChunks.get(0).getRole());
            assertEquals("", startChunks.get(0).getContent());
            assertEquals("Well ", startChunks.get(1).getContent());
            assertEquals("hello", startChunks.get(2).getContent());
            assertEquals("", startChunks.get(3).getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(formattedPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(completion1Expected, recordBodyMap.get("return_content"));
            assertEquals(customMetadata, recordBodyMap.get("custom_metadata"));

            // Continue
            // --------
            Stream<IndexedChatMessage> continueStream = chatStart.getSession().continueChatStream(
                    new ChatMessage("user", "Now in Italian!"),
                    llmParameters
            );
            List<IndexedChatMessage> continueChunks = continueStream.collect(Collectors.toList());

            // Completion
            assertEquals(4, continueChunks.size());
            assertEquals("assistant", continueChunks.get(0).getRole());
            assertEquals("", continueChunks.get(0).getContent());
            assertEquals("Bene ", continueChunks.get(1).getContent());
            assertEquals("ciao", continueChunks.get(2).getContent());
            assertEquals("", continueChunks.get(3).getContent());

            assertEquals(6, chatStart.getSession().getMessageHistory().size());
            assertEquals("Now in Italian!", chatStart.getSession().getMessageHistory().get(4).getContent());
            assertEquals("Bene ciao", chatStart.getSession().getMessageHistory().get(5).getContent());

            // Record call
            Map<String, Object> record2BodyMap = getCapturedBodyAsMap(mockedClient, 5, 4);
            assertEquals(formattedPrompt2Expected, record2BodyMap.get("prompt_content"));
            assertEquals(completion2Expected, record2BodyMap.get("return_content"));
        });
    }

    @Test
    public void chatDoesNotRecordWhenAskedNotTo() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mock2OpenAIChatStreamCalls(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey)),
                    DO_NOT_RECORD_PROCESSOR);
            Map<String, Object> llmParameters = Collections.emptyMap();

            // Start
            ChatStart<Stream<IndexedChatMessage>> chatStart = fpClient.startChatStream(
                    projectId,
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    llmParameters,
                    "latest"
            );

            List<IndexedChatMessage> startChunks = chatStart.getFirstCompletion().collect(Collectors.toList());

            // Completion
            assertEquals(4, startChunks.size());
            assertEquals("assistant", startChunks.get(0).getRole());
            assertEquals("", startChunks.get(0).getContent());
            assertEquals("Well ", startChunks.get(1).getContent());
            assertEquals("hello", startChunks.get(2).getContent());
            assertEquals("", startChunks.get(3).getContent());

            // Record call
            assertTrue(routeNotCalled(mockedClient, 2, "record"));
        });
    }


    @Test
    public void disallowsMessagesParam() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");

            try {
                Freeplay fpClient = new Freeplay(
                        freeplayApiKey,
                        baseUrl,
                        new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
                );
                fpClient.startChatStream(
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

    private void mock2OpenAIChatStreamCalls(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                    .thenReturn(response(200, getOpenAIChatResponseStreamMessages()))
                    .thenReturn(response(200, getOpenAIChatResponseStreamMessages2()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
