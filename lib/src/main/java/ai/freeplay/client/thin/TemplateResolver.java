package ai.freeplay.client.thin;

import ai.freeplay.client.thin.internal.dto.TemplatesDTO;

import java.util.concurrent.CompletableFuture;

public interface TemplateResolver {
    CompletableFuture<TemplatesDTO> getPrompts(String projectId, String environment);
}
