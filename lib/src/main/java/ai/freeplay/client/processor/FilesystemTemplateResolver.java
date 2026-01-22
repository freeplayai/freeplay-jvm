package ai.freeplay.client.processor;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.PromptTemplate;
import ai.freeplay.client.thin.internal.v2dto.TemplatesDTO;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
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
            TemplatesDTO prompts = thinResolver.getPrompts(projectId, environment).get();
            return prompts.getPromptTemplates().stream().map(template -> {
                Map<String, Object> params = template.getMetadata().getParams();
                params.put("model", template.getMetadata().getModel());
                return new PromptTemplate(
                        template.getPromptTemplateName(),
                        JSONUtil.toString(template.getContent()),
                        template.getMetadata().getFlavor(),
                        template.getPromptTemplateVersionId(),
                        template.getPromptTemplateId(),
                        template.getPromptTemplateVersionId(),
                        params
                );
            }).collect(toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new FreeplayConfigurationException(
                    format("Unable to get prompts for project %s, environment %s%n.", projectId, environment), e
            );
        }
    }
}
