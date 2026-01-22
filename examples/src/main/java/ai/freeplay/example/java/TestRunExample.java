package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.TestRun;

import java.util.Map;

import static java.lang.String.format;

public class TestRunExample {

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
        TestRun testRun = fpClient.createTestRun(
                projectId,
                "prod",
                "core-tests"
        );

        System.out.printf("Test Run: %s%n", testRun.getTestRunId());

        for (Map<String, Object> input : testRun.getInputs()) {
            CompletionSession session = testRun.createSession();
            CompletionResponse response = session.getCompletion("my-chat-start", input);
            System.out.printf("Completion for '%s': %s%n", input.get("question"), response.getContent());
        }
    }
}
