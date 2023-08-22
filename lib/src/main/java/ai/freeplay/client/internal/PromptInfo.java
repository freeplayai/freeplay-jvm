package ai.freeplay.client.internal;

import java.util.Map;

public class PromptInfo {
    private final String promptTemplateVersionId;
    private final String promptTemplateId;
    private final String formatType;
    private final String provider;
    private final String model;
    private final Map<String, Object> llmParameters;

    public PromptInfo(
            String promptTemplateVersionId,
            String promptTemplateId,
            String formatType,
            String provider,
            String model,
            Map<String, Object> llmParameters) {
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.promptTemplateId = promptTemplateId;
        this.formatType = formatType;
        this.provider = provider;
        this.model = model;
        this.llmParameters = llmParameters;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getFormatType() {
        return formatType;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Map<String, Object> getLLMParameters() {
        return llmParameters;
    }
}
