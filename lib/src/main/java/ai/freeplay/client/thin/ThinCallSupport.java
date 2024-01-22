package ai.freeplay.client.thin;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.thin.internal.model.Template;
import ai.freeplay.client.thin.internal.model.Templates;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.PromptUtils.getFinalTag;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;

class ThinCallSupport {
    private final HttpConfig httpConfig;
    private final TemplateResolver templateResolver;

    public ThinCallSupport(
            HttpConfig httpConfig,
            TemplateResolver templateResolver
    ) {
        this.httpConfig = httpConfig;
        this.templateResolver = templateResolver;
    }

    public static String createSessionId() throws FreeplayException {
        return randomUUID().toString();
    }

    public static String getActiveFlavorName(String callFlavorName, String templateFlavorName) {
        return callFlavorName != null ? callFlavorName : templateFlavorName;
    }

    public CompletableFuture<Template> getPrompt(
            String projectId,
            String templateName,
            String environment
    ) {
        return getPrompts(projectId, environment)
                .thenApply((Templates templates) ->
                        findPrompt(templates, templateName).orElseThrow(
                                () -> new FreeplayConfigurationException(
                                        format("Could not find template %s in environment %s.%n", templateName, environment)
                                )
                        )
                );
    }

    private CompletableFuture<Templates> getPrompts(String projectId, String tag) {
        String finalTag = getFinalTag(tag);
        return templateResolver.getPrompts(projectId, finalTag);
    }

    private Optional<Template> findPrompt(Templates templates, String templateName) {
        return templates
                .getTemplates()
                .stream()
                .filter(template -> template.getName().equals(templateName))
                .findFirst();
    }
}
