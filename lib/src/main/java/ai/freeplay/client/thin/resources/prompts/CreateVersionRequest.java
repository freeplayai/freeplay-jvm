package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.model.Provider;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;

import java.util.List;
import java.util.Map;

public class CreateVersionRequest {
    private final String projectId;
    private final String promptTemplateName;
    private final List<TemplateDTO.Message> templateMessages;
    private final String model;
    private final Provider provider;
    private final String versionName;
    private final String versionDescription;
    private final Map<String, Object> llmParameters;
    private final List<TemplateDTO.ToolSchema> toolSchema;
    private final List<String> environments;

    private CreateVersionRequest(Builder builder) {
        this.projectId = builder.projectId;
        this.promptTemplateName = builder.promptTemplateName;
        this.templateMessages = builder.templateMessages;
        this.model = builder.model;
        this.provider = builder.provider;
        this.versionName = builder.versionName;
        this.versionDescription = builder.versionDescription;
        this.llmParameters = builder.llmParameters;
        this.toolSchema = builder.toolSchema;
        this.environments = builder.environments;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getPromptTemplateName() {
        return promptTemplateName;
    }

    public List<TemplateDTO.Message> getTemplateMessages() {
        return templateMessages;
    }

    public String getModel() {
        return model;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getVersionDescription() {
        return versionDescription;
    }

    public Map<String, Object> getLlmParameters() {
        return llmParameters;
    }

    public List<TemplateDTO.ToolSchema> getToolSchema() {
        return toolSchema;
    }

    public List<String> getEnvironments() {
        return environments;
    }

    public static class Builder {
        private final String projectId;
        private final String promptTemplateName;
        private final List<TemplateDTO.Message> templateMessages;
        private final String model;
        private final Provider provider;
        private String versionName = null;
        private String versionDescription = null;
        private Map<String, Object> llmParameters = null;
        private List<TemplateDTO.ToolSchema> toolSchema = null;
        private List<String> environments = null;

        public Builder(
                String projectId,
                String promptTemplateName,
                List<TemplateDTO.Message> templateMessages,
                String model,
                Provider provider
        ) {
            this.projectId = projectId;
            this.promptTemplateName = promptTemplateName;
            this.templateMessages = templateMessages;
            this.model = model;
            this.provider = provider;
        }

        public Builder versionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        public Builder versionDescription(String versionDescription) {
            this.versionDescription = versionDescription;
            return this;
        }

        public Builder llmParameters(Map<String, Object> llmParameters) {
            this.llmParameters = llmParameters;
            return this;
        }

        public Builder toolSchema(List<TemplateDTO.ToolSchema> toolSchema) {
            this.toolSchema = toolSchema;
            return this;
        }

        public Builder environments(List<String> environments) {
            this.environments = environments;
            return this;
        }

        public CreateVersionRequest build() {
            return new CreateVersionRequest(this);
        }
    }
}
