package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.model.*;

import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;

public class ChatCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
        Map<String, Object> llmParameters = Collections.emptyMap();

        CompletionSession session = fpClient.createSession(projectId, "prod");

        CompletionResponse response = session.getCompletion(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );
        System.out.printf("Completion text: %s%n", response.getContent());

        ChatCompletionResponse chatResponse = session.getChatCompletion(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );

        chatResponse.getFirstChoice().ifPresent((ChatMessage message) -> {
            System.out.printf("Chat Completion text [%s]: %s%n", message.getRole(), chatResponse.getContent());
        });

    }
}
