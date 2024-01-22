package ai.freeplay.client.thin.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties({"project_version_id"})
public class Template {
    private String name;
    private String content;
    private String flavorName;
    private String promptTemplateId;
    private String promptTemplateVersionId;
    private Map<String, Object> params;

    public Template() {
    }

    public Template(
            String name,
            String content,
            String flavorName,
            String promptTemplateId,
            String promptTemplateVersionId,
            Map<String, Object> params
    ) {
        this.name = name;
        this.content = content;
        this.flavorName = flavorName;
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.params = params;
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

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Template template = (Template) o;
        return Objects.equals(name, template.name) && Objects.equals(content, template.content) && Objects.equals(flavorName, template.flavorName) && Objects.equals(promptTemplateId, template.promptTemplateId) && Objects.equals(promptTemplateVersionId, template.promptTemplateVersionId) && Objects.equals(params, template.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, content, flavorName, promptTemplateId, promptTemplateVersionId, params);
    }
}
