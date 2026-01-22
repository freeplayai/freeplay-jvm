package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.ChatStart;
import ai.freeplay.client.model.IndexedChatMessage;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;

public class OpenAIContinuousChatStreamCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
        Map<String, Object> llmParameters = Collections.emptyMap();
        ChatStart<Stream<IndexedChatMessage>> chatStart = fpClient.startChatStream(
                projectId,
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters,
                "latest"
        );
        chatStart.getFirstCompletion().forEach((IndexedChatMessage message) ->
                System.out.printf("Message [%s]: %s%n", message.getRole(), message.getContent())
        );

        chatStart.getSession().continueChatStream(
                new ChatMessage("user", "Now in Italian!"), llmParameters
        ).forEach((IndexedChatMessage message) ->
                System.out.printf("Message [%s]: %s%n", message.getRole(), message.getContent())
        );
    }
}
