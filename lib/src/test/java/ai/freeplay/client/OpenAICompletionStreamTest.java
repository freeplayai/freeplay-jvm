package ai.freeplay.client;

import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.IndexedChatMessage;
import ai.freeplay.client.processor.LLMCallInfo;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.RecordProcessor.DO_NOT_RECORD_PROCESSOR;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.MockMethods.routeNotCalled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("FieldCanBeLocal")
public class OpenAICompletionStreamTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";

    private final String chatCompletion1Expected = "Well hello";
    private final String formattedChatPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";
    private final String chatPromptWithInsertedMessage = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"Inserted Message\",\"role\":\"user\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";

    @Test
    public void chatReturnsFromCompletionCall() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<IndexedChatMessage> responseStream = session.getCompletionStream(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap()
            );
            List<IndexedChatMessage> chunks = responseStream.collect(Collectors.toList());

            // Completion
            assertEquals(4, chunks.size());
            assertEquals("assistant", chunks.get(0).getRole());
            assertEquals("", chunks.get(0).getContent());
            assertEquals("Well ", chunks.get(1).getContent());
            assertEquals("hello", chunks.get(2).getContent());
            assertEquals("", chunks.get(3).getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(formattedChatPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(chatCompletion1Expected, recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void completionStreamCustomMetadata() {
        Map<String, Object> customMetadata = Map.of("customer_id", "123", "batch_size", 456);
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );
            CompletionSession session = fpClient.createSession(projectId, "latest", customMetadata);
            Stream<IndexedChatMessage> responseStream = session.getCompletionStream(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    null,
                    null,
                    null
            );
            //noinspection unused
            List<IndexedChatMessage> chunks = responseStream.collect(Collectors.toList());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(customMetadata, recordBodyMap.get("custom_metadata"));
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void chatStreamHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
            );

            AtomicReference<LLMCallInfo> llmCallInfo = new AtomicReference<>();

            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<IndexedChatMessage> responseStream = session.getCompletionStream(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    null,
                    (Collection<ChatMessage> messages, LLMCallInfo info) -> {
                        llmCallInfo.set(info);
                        List<ChatMessage> newMessages = new ArrayList<>(messages);
                        newMessages.add(1, new ChatMessage("user", "Inserted Message"));
                        return newMessages;
                    }
            );

            @SuppressWarnings("unused")
            List<IndexedChatMessage> chunks = responseStream.collect(Collectors.toList());

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

    @Test
    public void chatDoesNotRecordWhenAskedNotTo() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, getChatPromptContentObjects(), Collections.emptyMap(), "openai_chat");
            mockOpenAIChatCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey)),
                    DO_NOT_RECORD_PROCESSOR);
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<IndexedChatMessage> responseStream = session.getCompletionStream(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap()
            );
            List<IndexedChatMessage> chunks = responseStream.collect(Collectors.toList());

            // Completion
            assertEquals(4, chunks.size());
            assertEquals("assistant", chunks.get(0).getRole());
            assertEquals("", chunks.get(0).getContent());
            assertEquals("Well ", chunks.get(1).getContent());
            assertEquals("hello", chunks.get(2).getContent());
            assertEquals("", chunks.get(3).getContent());

            // Record call
            assertTrue(routeNotCalled(mockedClient, 2, "record"));
        });
    }
}
