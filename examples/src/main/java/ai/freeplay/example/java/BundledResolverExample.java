package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.processor.FilesystemTemplateResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static ai.freeplay.client.Freeplay.Config;

public class BundledResolverExample {

    public static void main(String[] args) {
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
        String templateDirectory = System.getenv("FREEPLAY_TEMPLATE_DIRECTORY");

        Path templateDir = Paths.get(templateDirectory);

        Freeplay localClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .customerDomain(customerDomain)
                .templateResolver(new FilesystemTemplateResolver(templateDir))
                .providerConfigs(new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey)))
        );
        CompletionResponse localCompletion = localClient.getCompletion(
                projectId,
                "my-prompt-anthropic",
                Map.of("question", "why isn't my sink working?"),
                "prod"
        );
        System.out.printf("Local Completion text: %s%n", localCompletion.getContent());


        Freeplay apiClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .customerDomain(customerDomain)
                .providerConfigs(new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey)))
        );
        CompletionResponse apiCompletion = apiClient.getCompletion(
                projectId,
                "my-prompt-anthropic",
                Map.of("question", "why isn't my sink working?"),
                "prod"
        );
        System.out.printf("API completion text: %s%n", apiCompletion.getContent());
    }
}
