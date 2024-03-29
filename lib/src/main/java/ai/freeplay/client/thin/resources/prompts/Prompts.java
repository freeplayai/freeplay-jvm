package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavors;
import ai.freeplay.client.thin.internal.ThinCallSupport;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class Prompts {

    private final ThinCallSupport callSupport;

    public Prompts(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<TemplatePrompt> get(
            String projectId,
            String templateName,
            String environment
    ) {
        return callSupport
                .getPrompt(projectId, templateName, environment)
                .thenApply((TemplateDTO template) -> {
                    validateReturnedTemplate(template);

                    ChatFlavor flavor = Flavors.getFlavorByName(template.getMetadata().getFlavor());
                    String model = template.getMetadata().getModel();
                    HashMap<String, Object> params = new HashMap<>(template.getMetadata().getParams());
                    params.remove("model");

                    List<ChatMessage> messages = template.getContent().stream().map(message ->
                            new ChatMessage(message.getRole(), message.getContent())
                    ).collect(toList());

                    PromptInfo promptInfo = new PromptInfo(
                                    template.getPromptTemplateId(),
                                    template.getPromptTemplateVersionId(),
                                    template.getPromptTemplateName(),
                                    environment,
                                    params,
                                    flavor.getProvider(),
                                    model,
                                    template.getMetadata().getFlavor()
                            ).providerInfo(template.getMetadata().getProviderInfo());

                    return new TemplatePrompt(
                            promptInfo,
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
                .thenApply(boundPrompt -> boundPrompt.format(flavorName));
    }

    public <LLMFormat> CompletableFuture<FormattedPrompt<LLMFormat>> getFormatted(
            String projectId,
            String templateName,
            String environment,
            Map<String, Object> variables
    ) {
        return getBound(projectId, templateName, environment, variables)
                .thenApply(BoundPrompt::format);
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

    private void validateReturnedTemplate(TemplateDTO template) {
        if (template.getMetadata().getFlavor() == null) {
            throw new FreeplayConfigurationException(
                    "Flavor must be configured in the Freeplay UI. Unable to fulfill request.");
        }
        if (template.getMetadata().getModel() == null) {
            throw new FreeplayConfigurationException(
                    "Model must be configured in the Freeplay UI. Unable to fulfill request.");
        }
    }
}
