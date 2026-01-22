package ai.freeplay.client.processor;

import ai.freeplay.client.model.PromptTemplate;

import java.util.Collection;

public interface TemplateResolver {
    Collection<PromptTemplate> getPrompts(String projectId, String environment);
}
