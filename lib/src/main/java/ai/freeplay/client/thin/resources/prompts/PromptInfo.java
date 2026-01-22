package ai.freeplay.client.thin.resources.prompts;

import java.util.Map;
import java.util.Objects;

public class PromptInfo extends PromptVersionInfo {
    private final String promptTemplateId;
    private final String templateName;
    private final Map<String, Object> modelParameters;
    private Map<String, Object> providerInfo;
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
        super(promptTemplateVersionId, environment);
        this.promptTemplateId = promptTemplateId;
        this.templateName = templateName;
        this.modelParameters = modelParameters;
        this.provider = provider;
        this.model = model;
        this.flavorName = flavorName;
    }

    public PromptInfo providerInfo(Map<String, Object> providerInfo) {
        this.providerInfo = providerInfo;
        return this;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Map<String, Object> getModelParameters() {
        return modelParameters;
    }

    public Map<String, Object> getProviderInfo() {
        return providerInfo;
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
        if (!super.equals(o)) return false;
        PromptInfo that = (PromptInfo) o;
        return Objects.equals(promptTemplateId, that.promptTemplateId) && Objects.equals(templateName, that.templateName) && Objects.equals(modelParameters, that.modelParameters) && Objects.equals(providerInfo, that.providerInfo) && Objects.equals(provider, that.provider) && Objects.equals(model, that.model) && Objects.equals(flavorName, that.flavorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), promptTemplateId, templateName, modelParameters, providerInfo, provider, model, flavorName);
    }

    @Override
    public String toString() {
        return "PromptInfo{" +
                "promptTemplateId='" + promptTemplateId + '\'' +
                ", promptTemplateVersionId='" + getPromptTemplateVersionId() + '\'' +
                ", templateName='" + templateName + '\'' +
                ", environment='" + getEnvironment() + '\'' +
                ", modelParameters=" + modelParameters +
                ", providerInfo=" + providerInfo +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", flavorName='" + flavorName + '\'' +
                '}';
    }
}
