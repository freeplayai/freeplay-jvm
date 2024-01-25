package ai.freeplay.client.thin.internal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecordAPIPayload {

    private String sessionId;
    private String projectVersionId;
    private String promptTemplateId;
    private double startTime;
    private double endTime;
    private String tag;
    private Map<String, Object> inputs;
    private Map<String, Object> customMetadata;
    private String promptContent;
    private String returnContent;
    private String testRunId;
    private String provider;
    private String model;
    private Map<String, Object> llmParameters;
    private boolean isComplete;

    public RecordAPIPayload() {
    }

    public RecordAPIPayload(
            String sessionId,
            String projectVersionId,
            String promptTemplateId,
            double startTime,
            double endTime,
            String tag,
            Map<String, Object> inputs,
            Map<String, Object> customMetadata,
            String promptContent,
            String returnContent,
            boolean isComplete,
            String testRunId,
            String provider,
            String model,
            Map<String, Object> llmParameters
    ) {
        this.sessionId = sessionId;
        this.projectVersionId = projectVersionId;
        this.promptTemplateId = promptTemplateId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tag = tag;
        this.inputs = inputs;
        this.customMetadata = customMetadata;
        this.promptContent = promptContent;
        this.returnContent = returnContent;
        this.isComplete = isComplete;
        this.testRunId = testRunId;
        this.provider = provider;
        this.model = model;
        this.llmParameters = llmParameters;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getProjectVersionId() {
        return projectVersionId;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public String getTag() {
        return tag;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    public String getPromptContent() {
        return promptContent;
    }

    public String getReturnContent() {
        return returnContent;
    }

    @JsonProperty("is_complete")
    public boolean isComplete() {
        return isComplete;
    }

    public void setIsComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Map<String, Object> getLlmParameters() {
        return llmParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordAPIPayload payload = (RecordAPIPayload) o;
        return Double.compare(payload.startTime, startTime) == 0 && Double.compare(payload.endTime, endTime) == 0 && isComplete == payload.isComplete && Objects.equals(sessionId, payload.sessionId) && Objects.equals(projectVersionId, payload.projectVersionId) && Objects.equals(promptTemplateId, payload.promptTemplateId) && Objects.equals(tag, payload.tag) && Objects.equals(inputs, payload.inputs) && Objects.equals(customMetadata, payload.customMetadata) && Objects.equals(promptContent, payload.promptContent) && Objects.equals(returnContent, payload.returnContent) && Objects.equals(testRunId, payload.testRunId) && Objects.equals(provider, payload.provider) && Objects.equals(model, payload.model) && Objects.equals(llmParameters, payload.llmParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, projectVersionId, promptTemplateId, startTime, endTime, tag, inputs, customMetadata, promptContent, returnContent, isComplete, testRunId, provider, model, llmParameters);
    }

    @Override
    public String toString() {
        return "RecordAPIPayload{" +
                "sessionId='" + sessionId + '\'' +
                ", projectVersionId='" + projectVersionId + '\'' +
                ", promptTemplateId='" + promptTemplateId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", tag='" + tag + '\'' +
                ", inputs=" + inputs +
                ", customMetadata=" + customMetadata +
                ", promptContent='" + promptContent + '\'' +
                ", returnContent='" + returnContent + '\'' +
                ", isComplete=" + isComplete +
                ", testRunId='" + testRunId + '\'' +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", llmParameters=" + llmParameters +
                '}';
    }
}
