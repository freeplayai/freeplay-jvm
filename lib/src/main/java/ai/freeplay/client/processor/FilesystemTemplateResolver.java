package ai.freeplay.client.processor;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.model.PromptTemplate;
import ai.freeplay.client.thin.internal.model.Templates;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class FilesystemTemplateResolver implements TemplateResolver {

    private final ai.freeplay.client.thin.FilesystemTemplateResolver thinResolver;

    public FilesystemTemplateResolver(Path rootDirectory) {
        thinResolver = new ai.freeplay.client.thin.FilesystemTemplateResolver(rootDirectory);
    }

    @Override
    public Collection<PromptTemplate> getPrompts(String projectId, String environment) {
        try {
            Templates prompts = thinResolver.getPrompts(projectId, environment).get();
            return prompts.getTemplates().stream().map(template -> new PromptTemplate(
                    template.getName(),
                    template.getContent(),
                    template.getFlavorName(),
                    template.getPromptTemplateVersionId(),
                    template.getPromptTemplateId(),
                    template.getPromptTemplateVersionId(),
                    template.getParams()
            )).collect(toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new FreeplayConfigurationException(
                    format("Unable to get prompts for project %s, environment %s%n.", projectId, environment), e
            );
        }
    }
}
