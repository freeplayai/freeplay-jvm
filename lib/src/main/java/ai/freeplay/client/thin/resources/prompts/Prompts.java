package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavors;
import ai.freeplay.client.thin.internal.ThinCallSupport;
import ai.freeplay.client.thin.internal.dto.TemplateDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.JSONUtil.parseListOf;

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
