package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.flavor.AnthropicChatFlavor;
import ai.freeplay.client.model.CompletionResponse;

import java.util.Map;

import static java.lang.String.format;

public class AnthropicComplexTemplateCompletion {

    public static void main(String[] args) {
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(
                freeplayApiKey,
                baseUrl,
                new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey)),
                new AnthropicChatFlavor(),
                Map.of(
                        "max_tokens", 50,
                        "model", "claude-2.1"
                ),
                new HttpConfig());
        CompletionResponse completion = fpClient.getCompletion(
                projectId,
                "my-prompt-anthropic",
                Map.of("question", "why isn't my sink working?"),
                "latest"
        );
        System.out.printf("Completion text: %s%n", completion.getContent());
    }
}
