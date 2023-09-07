package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.model.*;

import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;

public class OpenAIContinuousChatCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
        Map<String, Object> llmParameters = Collections.emptyMap();
        ChatStart<IndexedChatMessage> chatStart = fpClient.startChat(
                projectId,
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters,
                "latest"
        );
        System.out.printf("Completion text: %s%n", chatStart.getFirstCompletion().getContent());

        ChatCompletionResponse response = chatStart.getSession().continueChat(
                new ChatMessage("user", "Now in Italian!"), llmParameters);
        System.out.printf("Second message: %s%n", response.getContent());
    }
}
