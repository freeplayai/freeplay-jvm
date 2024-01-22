package ai.freeplay.client.thin;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavors;
import ai.freeplay.client.thin.internal.model.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.JSONUtil.parseListOf;

public class Freeplay {
    private final ThinCallSupport callSupport;

    private final Prompts prompts;

    public Freeplay(FreeplayConfig config) {
        config.validate();
        this.callSupport = new ThinCallSupport(
                null,
                config.templateResolver
        );
        prompts = new Prompts();
    }

    public Prompts prompts() {
        return prompts;
    }

    public class Prompts {

        public CompletableFuture<TemplatePrompt> get(
                String projectId,
                String templateName,
                String environment
        ) {
            return callSupport
                    .getPrompt(projectId, templateName, environment)
                    .thenApply((Template template) -> {
                        validateReturnedTemplate(template);

                        ChatFlavor flavor = Flavors.getFlavorByName(template.getFlavorName());
                        String model = template.getParams().get("model").toString();
                        HashMap<String, Object> params = new HashMap<>(template.getParams());
                        params.remove("model");

                        List<ChatMessage> messages = parseListOf(template.getContent(), ChatMessage.class);

                        return new TemplatePrompt(
                                new PromptInfo(
                                        template.getPromptTemplateId(),
                                        template.getPromptTemplateVersionId(),
                                        template.getName(),
                                        environment,
                                        params,
                                        flavor.getProvider(),
                                        model,
                                        template.getFlavorName()
                                ),
                                messages
                        );
                    });
        }

        public <LLMFormat> CompletableFuture<FormattedPrompt<LLMFormat>> getFormatted(
                String projectId,
                String templateName,
                String environment,
                Map<String, Object> variables,
                String flavorName
        ) {
            return getBound(projectId, templateName, environment, variables)
                    .thenApply(boundPrompt ->
                            new FormattedPrompt<>(
                                    boundPrompt.getPromptInfo(),
                                    boundPrompt.format(flavorName)
                            )
                    );
        }

        private CompletableFuture<BoundPrompt> getBound(
                String projectId,
                String templateName,
                String environment,
                Map<String, Object> variables
        ) {
            return get(projectId, templateName, environment)
                    .thenApply(templatePrompt -> templatePrompt.bind(variables));
        }

        private void validateReturnedTemplate(Template template) {
            if (template.getFlavorName() == null) {
                throw new FreeplayConfigurationException(
                        "Flavor must be configured in the Freeplay UI. Unable to fulfill request.");
            }
            if (!template.getParams().containsKey("model")) {
                throw new FreeplayConfigurationException(
                        "Model must be configured in the Freeplay UI. Unable to fulfill request.");
            }
        }
    }

    public static FreeplayConfig Config() {
        return new FreeplayConfig();
    }

    public static class FreeplayConfig {
        private String freeplayAPIKey = null;
        private String baseUrl = null;
        private HttpConfig httpConfig = new HttpConfig();
        private TemplateResolver templateResolver = null;

        public FreeplayConfig freeplayAPIKey(String freeplayAPIKey) {
            this.freeplayAPIKey = freeplayAPIKey;
            return this;
        }

        public FreeplayConfig customerDomain(String domain) {
            return baseUrl(String.format("https://%s.freeplay.ai/api", domain));
        }

        public FreeplayConfig baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public FreeplayConfig httpConfig(HttpConfig httpConfig) {
            this.httpConfig = httpConfig;
            return this;
        }

        public FreeplayConfig templateResolver(TemplateResolver templateResolver) {
            this.templateResolver = templateResolver;
            return this;
        }

        public void validate() {
            if (templateResolver == null) {
                if (freeplayAPIKey == null || baseUrl == null) {
                    throw new FreeplayConfigurationException("Either a TemplateResolver must be configured, " +
                            "or the Freeplay API key and base URL must be configured.");
                } else {
                    templateResolver = new APITemplateResolver(baseUrl, freeplayAPIKey, httpConfig);
                }
            }
        }
    }
}
