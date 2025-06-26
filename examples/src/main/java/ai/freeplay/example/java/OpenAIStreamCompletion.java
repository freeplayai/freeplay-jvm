package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.IndexedChatMessage;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static ai.freeplay.client.CompletionFeedback.POSITIVE_FEEDBACK;
import static java.lang.String.format;

public class OpenAIStreamCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(
                freeplayApiKey,
                baseUrl,
                new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
        );
        Map<String, Object> llmParameters = Collections.emptyMap();

        CompletionSession session = fpClient.createSession(projectId, "prod");

        Stream<IndexedChatMessage> completionStream = session.getCompletionStream(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );
        completionStream.forEach((IndexedChatMessage chunk) -> {
            System.out.printf("Message [%s]: %s%n", chunk.getRole(), chunk.getContent());
            if (chunk.isLast()) {
                fpClient.recordCompletionFeedback(
                        projectId,
                        chunk.getCompletionId(),
                        Map.of("feedback", POSITIVE_FEEDBACK)
                );
            }
        });
    }
}
