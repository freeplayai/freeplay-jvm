package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;

public class OpenAIStreamCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
        Map<String, Object> llmParameters = Collections.emptyMap();

        CompletionSession session = fpClient.createSession(projectId, "prod");

        Stream<ChatMessage> completionStream = session.getCompletionStream(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );
        completionStream.forEach((ChatMessage chunk) -> {
            System.out.printf("Message [%s]: %s%n", chunk.getRole(), chunk.getContent());
        });

        Stream<CompletionResponse> textStream = session.getCompletionStream(
                "my-prompt",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );
        textStream.forEach((CompletionResponse chunk) -> {
            System.out.printf("Message [TEXT]: %s%n", chunk.getContent());
        });
    }
}
