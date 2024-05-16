package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecordDTO {

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
    private String testCaseId;
    private String provider;
    private String model;
    private Map<String, Object> llmParameters;
    private Map<String, Object> providerInfo;
    private boolean isComplete;
    private Map<String, String> functionCallResponse;
    private Map<String, Object> evalResults;

    public RecordDTO() {
    }

    public RecordDTO(
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
            String testCaseId,
            String provider,
            String model,
            Map<String, Object> llmParameters,
            Map<String, Object> providerInfo,
            Map<String, String> functionCallResponse,
            Map<String, Object> evalResults
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
        this.testCaseId = testCaseId;
        this.provider = provider;
        this.model = model;
        this.llmParameters = llmParameters;
        this.providerInfo = providerInfo;
        this.functionCallResponse = functionCallResponse;
        this.evalResults = evalResults;
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

    public String getTestCaseId() {
        return testCaseId;
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

    public Map<String, Object> getProviderInfo() {
        return providerInfo;
    }

    public Map<String, String> getFunctionCallResponse() {
        return functionCallResponse;
    }

    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordDTO recordDTO = (RecordDTO) o;
        return Double.compare(startTime, recordDTO.startTime) == 0 && Double.compare(endTime, recordDTO.endTime) == 0 && isComplete == recordDTO.isComplete && Objects.equals(sessionId, recordDTO.sessionId) && Objects.equals(projectVersionId, recordDTO.projectVersionId) && Objects.equals(promptTemplateId, recordDTO.promptTemplateId) && Objects.equals(tag, recordDTO.tag) && Objects.equals(inputs, recordDTO.inputs) && Objects.equals(customMetadata, recordDTO.customMetadata) && Objects.equals(promptContent, recordDTO.promptContent) && Objects.equals(returnContent, recordDTO.returnContent) && Objects.equals(testRunId, recordDTO.testRunId) && Objects.equals(testCaseId, recordDTO.testCaseId) && Objects.equals(provider, recordDTO.provider) && Objects.equals(model, recordDTO.model) && Objects.equals(llmParameters, recordDTO.llmParameters) && Objects.equals(providerInfo, recordDTO.providerInfo) && Objects.equals(functionCallResponse, recordDTO.functionCallResponse) && Objects.equals(evalResults, recordDTO.evalResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, projectVersionId, promptTemplateId, startTime, endTime, tag, inputs, customMetadata, promptContent, returnContent, testRunId, testCaseId, provider, model, llmParameters, providerInfo, isComplete, functionCallResponse, evalResults);
    }

    @Override
    public String toString() {
        return "RecordDTO{" +
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
                ", testRunId='" + testRunId + '\'' +
                ", testCaseId='" + testCaseId + '\'' +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", llmParameters=" + llmParameters +
                ", providerInfo=" + providerInfo +
                ", isComplete=" + isComplete +
                ", functionCallResponse=" + functionCallResponse +
                ", evalResults=" + evalResults +
                '}';
    }
}
