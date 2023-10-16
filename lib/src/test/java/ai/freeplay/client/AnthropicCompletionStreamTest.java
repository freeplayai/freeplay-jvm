package ai.freeplay.client;

import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.PromptProcessors.testTextProcessor;
import static org.junit.Assert.assertEquals;

public class AnthropicCompletionStreamTest extends HttpClientTestBase {

    @Test
    public void textCompletionReturnsValue() {
        String templateName = "my-prompt";
        String textPromptContent = "Answer this question: {{question}}";

        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<CompletionResponse> responseStream = session.getCompletionStream(
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    )
            );

            List<CompletionResponse> chunks = responseStream.collect(Collectors.toList());

            // Completion
            assertEquals(4, chunks.size());
            assertEquals(" Oh", chunks.get(0).getContent());
            assertEquals(" dear", chunks.get(1).getContent());
            assertEquals(",", chunks.get(2).getContent());
            assertEquals(" really", chunks.get(3).getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals("Answer this question: why isn't my sink working?", recordBodyMap.get("prompt_content"));
            assertEquals(" Oh dear, really", recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void textCompletionHandlesProcessor() {
        String templateName = "my-prompt";
        String textPromptContent = "Answer this question: {{question}}";
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<CompletionResponse> responseStream = session.getCompletionStream(
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    ),
                    null,
                    testTextProcessor
            );

            @SuppressWarnings("unused")
            List<CompletionResponse> chunks = responseStream.collect(Collectors.toList());

            // Modified OpenAI call
            Map<String, Object> openAiRequestBody = getCapturedBodyAsMap(mockedClient, 4, 2);
            assertEquals(
                    "\n\nHuman: PREPENDED_TEXT Answer this question: why isn't my sink working? \n\nAssistant:",
                    openAiRequestBody.get("prompt"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(
                    "PREPENDED_TEXT Answer this question: why isn't my sink working?",
                    recordBodyMap.get("prompt_content"));

        });
    }
}
