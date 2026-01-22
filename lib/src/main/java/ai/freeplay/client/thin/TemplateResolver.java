package ai.freeplay.client.thin;

import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.internal.v2dto.TemplatesDTO;

import java.util.concurrent.CompletableFuture;

public interface TemplateResolver {
    CompletableFuture<TemplatesDTO> getPrompts(String projectId, String environment);

    CompletableFuture<TemplateDTO> getPrompt(String projectId, String templateName, String environment);

    CompletableFuture<TemplateDTO> getPromptByVersionId(String projectId, String templateId, String templateVersionId);
}
