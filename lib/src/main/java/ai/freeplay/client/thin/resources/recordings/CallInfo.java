package ai.freeplay.client.thin.resources.recordings;

import ai.freeplay.client.thin.resources.prompts.PromptInfo;

import java.time.Instant;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class CallInfo {
    private final String provider;
    private final String model;
    private final double startTime;
    private final double endTime;
    private final Map<String, Object> modelParameters;
    private Map<String, Object> customMetadata;

    public static double instantToDouble(Instant instant) {
        return Double.parseDouble(instant.getEpochSecond() + "." + instant.getNano());
    }

    public static CallInfo from(PromptInfo promptInfo, long startTime, long endTime) {
        return new CallInfo(
                promptInfo.getProvider(),
                promptInfo.getModel(),
                startTime,
                endTime,
                promptInfo.getModelParameters()
        );
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
        this.startTime = instantToDouble(Instant.ofEpochMilli(startTime));
        this.endTime = instantToDouble(Instant.ofEpochMilli(endTime));
        this.modelParameters = modelParameters;
    }

    @SuppressWarnings("unused")
    public CallInfo customMetadata(Map<String, Object> customMetadata) {
        this.customMetadata = customMetadata;
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

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }
}
