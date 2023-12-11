package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.IndexedChatMessage;

import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;

public class AnthropicStreamCompletion {

    public static void main(String[] args) {
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
        CompletionSession session = fpClient.createSession(projectId, "prod");
        Stream<IndexedChatMessage> completion = session.getCompletionStream(
                "my-prompt-anthropic",
                Map.of("question", "why isn't my sink working?")
        );

        completion.forEach((IndexedChatMessage chunk) ->
                System.out.printf("Completion text: '%s'%n", chunk.getContent())
        );
    }
}