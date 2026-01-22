package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;

import java.util.Collections;
import java.util.Map;

import static ai.freeplay.client.RecordProcessor.DO_NOT_RECORD_PROCESSOR;
import static java.lang.String.format;

public class OpenAIChatCompletionNoRecord {

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
                DO_NOT_RECORD_PROCESSOR
        );
        Map<String, Object> llmParameters = Collections.emptyMap();

        CompletionSession session = fpClient.createSession(projectId, "prod");

        CompletionResponse response = session.getCompletion(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters
        );
        System.out.printf("Completion text: %s%n", response.getContent());
    }
}
