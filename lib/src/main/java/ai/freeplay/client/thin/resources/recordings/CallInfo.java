package ai.freeplay.client.thin.resources.recordings;

import ai.freeplay.client.thin.resources.prompts.PromptInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CallInfo {
    private final String provider;
    private final String model;
    private final double startTime;
    private final double endTime;
    private final Map<String, Object> modelParameters;
    private Map<String, Object> providerInfo;
    private UsageTokens usage;
    private ApiStyle apiStyle;

    public static CallInfo from(PromptInfo promptInfo, long startTime, long endTime) {
        return new CallInfo(
                promptInfo.getProvider(),
                promptInfo.getModel(),
                startTime,
                endTime,
                promptInfo.getModelParameters()
        ).providerInfo(promptInfo.getProviderInfo());
    }

    public CallInfo(
            String provider,
            String model,
            long startTime,
            long endTime,
            Map<String, Object> modelParameters
    ) {
        this.provider = provider;
        this.model = model;
        this.startTime = startTime / 1000.0;
        this.endTime = endTime / 1000.0;
        this.modelParameters = modelParameters;
    }


    public CallInfo providerInfo(Map<String, Object> providerInfo) {
        this.providerInfo = providerInfo;
        return this;
    }

    public CallInfo usage(UsageTokens usage) {
        this.usage = usage;
        return this;
    }

    public CallInfo apiStyle(ApiStyle apiStyle) {
        this.apiStyle = apiStyle;
        return this;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public Map<String, Object> getModelParameters() {
        return modelParameters;
    }

    public Map<String, Object> getProviderInfo() {
        return providerInfo;
    }

    public UsageTokens getUsage() {
        return usage;
    }

    public ApiStyle getApiStyle() {
        return apiStyle;
    }

    public enum ApiStyle {
        BATCH, DEFAULT
    }


    public static class UsageTokens {
        private final Integer completionTokens;
        private final Integer promptTokens;

        public UsageTokens(
                Integer promptTokens,
                Integer completionTokens
        ) {
            this.completionTokens = completionTokens;
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }
    }

}
