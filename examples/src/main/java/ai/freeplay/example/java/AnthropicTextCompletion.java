package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.model.CompletionResponse;

import java.util.Map;

import static java.lang.String.format;

public class AnthropicTextCompletion {

    public static void main(String[] args) {
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
        CompletionResponse completion = fpClient.getCompletion(
                projectId,
                "my-prompt-anthropic",
                Map.of("question", "why isn't my sink working?"),
                "prod"
        );
        System.out.printf("Completion text: %s%n", completion.getContent());
    }
}
