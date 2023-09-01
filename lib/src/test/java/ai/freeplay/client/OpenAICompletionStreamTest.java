package ai.freeplay.client;

import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.IndexedChatMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@SuppressWarnings("FieldCanBeLocal")
public class OpenAICompletionStreamTest {

    private final String templateName = "my-prompt";

    private final String textPromptContent = "Answer this question: {{question}}";
    private final String textCompletion = "Well hello";

    private final String chatCompletion1Expected = "Well hello";
    private final String formattedChatPromptExpected = "[{\"content\":\"You are a support agent.\",\"role\":\"system\"},{\"content\":\"How may I help you?\",\"role\":\"assistant\"},{\"content\":\"why isn't my sink working?\",\"role\":\"user\"}]";

    private HttpClient mockedClient;

    @Before
    public void beforeEach() {
        mockedClient = mock(HttpClient.class);
    }

    @Test
    public void textReturnsFromCompletionCall() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, MODEL_TEXT_DAVINCI_003, templateName, textPromptContent, "openai_text");
        mockOpenAITextCallStream(mockedClient);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<CompletionResponse> responseStream = session.getCompletionStream(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    null,
                    null
            );
            List<CompletionResponse> chunks = responseStream.collect(Collectors.toList());

            // Completion
            assertEquals(3, chunks.size());
            assertEquals("Well ", chunks.get(0).getContent());
            assertEquals("hello", chunks.get(1).getContent());
            assertEquals("", chunks.get(2).getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals("Answer this question: why isn't my sink working?", recordBodyMap.get("prompt_content"));
            assertEquals(textCompletion, recordBodyMap.get("return_content"));
        }
    }

    @Test
    public void chatReturnsFromCompletionCall() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, MODEL_GPT_TURBO_35, templateName, getChatPromptContent());
        mockOpenAIChatCallStream(mockedClient);

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<IndexedChatMessage> responseStream = session.getCompletionStream(
                    templateName,
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    null,
                    null
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
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(formattedChatPromptExpected, recordBodyMap.get("prompt_content"));
            assertEquals(chatCompletion1Expected, recordBodyMap.get("return_content"));
        }
    }
}
