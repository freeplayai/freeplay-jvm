package ai.freeplay.client.processor;

import ai.freeplay.client.model.Provider;

import java.util.Map;

public class LLMCallInfo {
    private final Map<String, Object> llmParameters;
    private final Provider provider;

    public LLMCallInfo(Provider provider, Map<String, Object> llmParameters) {
        this.llmParameters = llmParameters;
        this.provider = provider;
    }

    public Provider getProvider() {
        return provider;
    }

    public Map<String, Object> getLLMParameters() {
        return llmParameters;
    }
}
