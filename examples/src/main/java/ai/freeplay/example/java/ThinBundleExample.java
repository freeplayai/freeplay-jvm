package ai.freeplay.example.java;

import ai.freeplay.client.thin.FilesystemTemplateResolver;
import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.callAnthropic;
import static java.lang.String.format;

public class ThinBundleExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String apiRoot = System.getenv("FREEPLAY_API_URL");
        String baseUrl = format("%s/api", apiRoot);
        String templateDirectory = System.getenv("FREEPLAY_TEMPLATE_DIRECTORY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .baseUrl(baseUrl)
                .templateResolver(new FilesystemTemplateResolver(Paths.get(templateDirectory)))
        );

        Map<String, Object> variables = Map.of("location", "New York");

        fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        projectId,
                        "my-anthropic-prompt",
                        "latest",
                        variables,
                        null
                ).thenCompose((FormattedPrompt<List<ChatMessage>> formattedPrompt) ->
                        callAnthropic(
                                objectMapper,
                                anthropicApiKey,
                                formattedPrompt.getPromptInfo().getModel(),
                                formattedPrompt.getPromptInfo().getModelParameters(),
                                formattedPrompt.getFormattedPrompt(),
                                formattedPrompt.getSystemContent().orElse(null),
                                formattedPrompt.getToolSchema()
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
}
