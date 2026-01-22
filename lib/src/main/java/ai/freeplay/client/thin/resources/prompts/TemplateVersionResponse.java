package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;

import java.util.List;
import java.util.Map;

public class TemplateVersionResponse {
    private final String promptTemplateId;
    private final String promptTemplateVersionId;
    private final String promptTemplateName;
    private final String versionName;
    private final String versionDescription;
    private final PromptTemplateMetadata metadata;
    private final int formatVersion;
    private final String projectId;
    private final List<TemplateDTO.Message> content;
    private final List<TemplateDTO.ToolSchema> toolSchema;

    public TemplateVersionResponse(
            String promptTemplateId,
            String promptTemplateVersionId,
            String promptTemplateName,
            String versionName,
            String versionDescription,
            PromptTemplateMetadata metadata,
            int formatVersion,
            String projectId,
            List<TemplateDTO.Message> content,
            List<TemplateDTO.ToolSchema> toolSchema
    ) {
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.promptTemplateName = promptTemplateName;
        this.versionName = versionName;
        this.versionDescription = versionDescription;
        this.metadata = metadata;
        this.formatVersion = formatVersion;
        this.projectId = projectId;
        this.content = content;
        this.toolSchema = toolSchema;
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

    public PromptTemplateMetadata getMetadata() {
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

    public static class PromptTemplateMetadata {
        private final String provider;
        private final String flavor;
        private final String model;
        private final Map<String, Object> params;
        private final Map<String, Object> providerInfo;

        public PromptTemplateMetadata(
                String provider,
                String flavor,
                String model,
                Map<String, Object> params,
                Map<String, Object> providerInfo
        ) {
            this.provider = provider;
            this.flavor = flavor;
            this.model = model;
            this.params = params;
            this.providerInfo = providerInfo;
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
