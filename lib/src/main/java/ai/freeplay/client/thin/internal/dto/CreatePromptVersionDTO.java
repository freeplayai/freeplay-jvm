package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreatePromptVersionDTO {
    private String promptTemplateName;
    private List<TemplateDTO.Message> templateMessages;
    private String model;
    private String provider;
    private String versionName;
    private String versionDescription;
    private Map<String, Object> llmParameters;
    private List<TemplateDTO.ToolSchema> toolSchema;
    private List<String> environments;

    public CreatePromptVersionDTO() {
    }

    public CreatePromptVersionDTO(
            String promptTemplateName,
            List<TemplateDTO.Message> templateMessages,
            String model,
            String provider,
            String versionName,
            String versionDescription,
            Map<String, Object> llmParameters,
            List<TemplateDTO.ToolSchema> toolSchema,
            List<String> environments
    ) {
        this.promptTemplateName = promptTemplateName;
        this.templateMessages = templateMessages;
        this.model = model;
        this.provider = provider;
        this.versionName = versionName;
        this.versionDescription = versionDescription;
        this.llmParameters = llmParameters;
        this.toolSchema = toolSchema;
        this.environments = environments;
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

    public String getProvider() {
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
}
