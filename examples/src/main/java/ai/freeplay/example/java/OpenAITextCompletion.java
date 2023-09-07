package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.model.CompletionResponse;

import java.util.Map;

import static java.lang.String.format;

public class OpenAITextCompletion {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
        CompletionResponse completion = fpClient.getCompletion(
                projectId,
                "my-prompt",
                Map.of("question", "why isn't my sink working?"),
                Map.of("model", "text-davinci-003"),
                "latest"
        );
        System.out.printf("Completion text: %s%n", completion.getContent());
    }
}
