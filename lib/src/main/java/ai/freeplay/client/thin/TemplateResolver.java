package ai.freeplay.client.thin;

import ai.freeplay.client.thin.internal.model.Templates;

import java.util.concurrent.CompletableFuture;

public interface TemplateResolver {
    CompletableFuture<Templates> getPrompts(String projectId, String environment);
}
