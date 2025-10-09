package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TemplateVersionResponseDTO {
    private String promptTemplateId;
    private String promptTemplateVersionId;
    private String promptTemplateName;
    private String versionName;
    private String versionDescription;
    private MetadataDTO metadata;
    private int formatVersion;
    private String projectId;
    private List<TemplateDTO.Message> content;
    private List<TemplateDTO.ToolSchema> toolSchema;

    public TemplateVersionResponseDTO() {
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public String getPromptTemplateName() {
        return promptTemplateName;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getVersionDescription() {
        return versionDescription;
    }

    public MetadataDTO getMetadata() {
        return metadata;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public String getProjectId() {
        return projectId;
    }

    public List<TemplateDTO.Message> getContent() {
        return content;
    }

    public List<TemplateDTO.ToolSchema> getToolSchema() {
        return toolSchema;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MetadataDTO {
        private String provider;
        private String flavor;
        private String model;
        private Map<String, Object> params;
        private Map<String, Object> providerInfo;

        public MetadataDTO() {
        }

        public String getProvider() {
            return provider;
        }

        public String getFlavor() {
            return flavor;
        }

        public String getModel() {
            return model;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public Map<String, Object> getProviderInfo() {
            return providerInfo;
        }
    }
}
