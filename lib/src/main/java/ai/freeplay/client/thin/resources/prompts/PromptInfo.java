package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.thin.resources.recordings.CallInfo;

import java.util.Map;
import java.util.Objects;

public class PromptInfo {
    private final String promptTemplateId;
    private final String promptTemplateVersionId;
    private final String templateName;
    private final String environment;
    private final Map<String, Object> modelParameters;
    private final String provider;
    private final String model;
    private final String flavorName;

    public PromptInfo(
            String promptTemplateId,
            String promptTemplateVersionId,
            String templateName,
            String environment,
            Map<String, Object> modelParameters,
            String provider,
            String model,
            String flavorName
    ) {
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.templateName = templateName;
        this.environment = environment;
        this.modelParameters = modelParameters;
        this.provider = provider;
        this.model = model;
        this.flavorName = flavorName;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getEnvironment() {
        return environment;
    }

    public Map<String, Object> getModelParameters() {
        return modelParameters;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getFlavorName() {
        return flavorName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromptInfo that = (PromptInfo) o;
        return Objects.equals(promptTemplateId, that.promptTemplateId) && Objects.equals(promptTemplateVersionId, that.promptTemplateVersionId) && Objects.equals(templateName, that.templateName) && Objects.equals(environment, that.environment) && Objects.equals(modelParameters, that.modelParameters) && Objects.equals(provider, that.provider) && Objects.equals(model, that.model) && Objects.equals(flavorName, that.flavorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptTemplateId, promptTemplateVersionId, templateName, environment, modelParameters, provider, model, flavorName);
    }
}
