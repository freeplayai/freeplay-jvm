package ai.freeplay.example.java;

import ai.freeplay.client.thin.FilesystemTemplateResolver;
import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class ThinBundleExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
        String templateDirectory = System.getenv("FREEPLAY_TEMPLATE_DIRECTORY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .customerDomain(customerDomain)
                .templateResolver(new FilesystemTemplateResolver(Paths.get(templateDirectory)))
        );

        Map<String, Object> variables = Map.of("question", "Why isn't my window working?");

        fpClient.prompts()
                .<String>getFormatted(
                        projectId,
                        "my-prompt-anthropic",
                        "prod",
                        variables,
                        null
                ).thenCompose((FormattedPrompt<String> formattedPrompt) ->
                        callAnthropic(
                                anthropicApiKey,
                                formattedPrompt.getPromptInfo().getModel(),
                                formattedPrompt.getPromptInfo().getModelParameters(),
                                formattedPrompt.getFormattedPrompt()
                        )
                ).thenAccept((HttpResponse<String> response) ->
                        System.out.printf("Got response from Anthropic [%s]: %s%n", response.statusCode(), response.body())
                )
                .exceptionally(exception -> {
                    System.out.println("Got exception: " + exception.getMessage());
                    return null;
                })
                .join();
    }

    private static CompletableFuture<HttpResponse<String>> callAnthropic(
            String anthropicApiKey,
            String model,
            Map<String, Object> llmParameters,
            String prompt
    ) {
        try {
            String anthropicCompletionsURL = "https://api.anthropic.com/v1/complete";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("prompt", prompt);
            bodyMap.putAll(llmParameters);
            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(new URI(anthropicCompletionsURL))
                    .header("accept", "application/json")
                    .header("anthropic-version", "2023-06-01")
                    .header("x-api-key", anthropicApiKey)
                    .POST(ofString(body));

            return HttpClient.newBuilder()
                    .build()
                    .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
