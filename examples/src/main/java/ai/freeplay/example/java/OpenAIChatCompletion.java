package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;

public class OpenAIChatCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(
                freeplayApiKey,
                baseUrl,
                new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey)),
                Collections.emptyMap(),
                new HttpConfig(Duration.ofMillis(10_000))
        );
        Map<String, Object> llmParameters = Collections.emptyMap();

        CompletionSession session = fpClient.createSession(
                projectId,
                "prod",
                Map.of("customer_id", 123, "is_good", "true"));

        CompletionResponse response = session.getCompletion(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );
        System.out.printf("Completion text: %s%n", response.getContent());

        fpClient.recordCompletionFeedback(
                projectId,
                response.getCompletionId(),
                Map.of("feedback", "freeplay-positive-feedback")
        );

        ChatCompletionResponse chatResponse = session.getChatCompletion(
                "media",
                Map.of("question", "why else isn't my sink working?"),
                llmParameters
        );

        chatResponse.getFirstChoice().ifPresent((ChatMessage message) ->
                System.out.printf("Chat Completion content [%s]: %s%n", message.getRole(), chatResponse.getContent()));
    }
}
