package ai.freeplay.client.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class PromptTemplate {
    private final String name;
    private final String content;
    private final String flavorName;
    private final String projectVersionId;
    private final String promptTemplateId;
    private final String promptTemplateVersionId;

    private final Map<String, Object> llmParameters;

    public PromptTemplate(
            String name,
            String content,
            String flavorName,
            String projectVersionId,
            String promptTemplateId,
            String promptTemplateVersionId,
            Map<String, Object> llmParameters) {
        this.name = name;
        this.content = content;
        this.flavorName = flavorName;
        this.projectVersionId = projectVersionId;
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.llmParameters = llmParameters != null ? llmParameters : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getFlavorName() {
        return flavorName;
    }

    public String getProjectVersionId() {
        return projectVersionId;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public Map<String, Object> getLLMParameters() {
        return llmParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromptTemplate template = (PromptTemplate) o;
        return Objects.equals(name, template.name) &&
                Objects.equals(content, template.content) &&
                Objects.equals(flavorName, template.flavorName) &&
                Objects.equals(promptTemplateId, template.promptTemplateId) &&
                Objects.equals(promptTemplateVersionId, template.promptTemplateVersionId) &&
                Objects.equals(llmParameters, template.llmParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, content, flavorName, promptTemplateId, promptTemplateVersionId, llmParameters);
    }

    @Override
    public String toString() {
        return "PromptTemplate{" +
                "name='" + name + '\'' +
                ", content='" + content + '\'' +
                ", flavorName='" + flavorName + '\'' +
                ", projectVersionId='" + projectVersionId + '\'' +
                ", promptTemplateId='" + promptTemplateId + '\'' +
                ", promptTemplateVersionId='" + promptTemplateVersionId + '\'' +
                ", llmParameters=" + llmParameters +
                '}';
    }
}
