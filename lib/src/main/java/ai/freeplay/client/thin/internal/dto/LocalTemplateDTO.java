package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/**
 * This is a template similar to what we get back from the API, but when we load it locally ('bundled' from the
 * FilesystemTemplateResolver). It has a little different structure.
 */
@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties({"project_version_id"})
public class LocalTemplateDTO {
    private String promptTemplateId;
    private String promptTemplateVersionId;
    private String name;
    private String content;
    private Metadata metadata;

    public LocalTemplateDTO() {
    }

    public LocalTemplateDTO(
            String name,
            String content,

            String promptTemplateId,
            String promptTemplateVersionId,
            Metadata metadata
    ) {
        this.name = name;
        this.content = content;
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Metadata {
        private String flavorName;
        private Map<String, Object> params;

        public Metadata() {
        }

        public Metadata(String flavorName, Map<String, Object> params) {
            this.flavorName = flavorName;
            this.params = params;
        }

        public String getFlavorName() {
            return flavorName;
        }

        public Map<String, Object> getParams() {
            return params;
        }
    }
}
