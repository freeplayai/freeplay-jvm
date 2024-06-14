package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecordDTO {

    private List<ChatMessage> messages;
    private Map<String, Object> inputs;
    private SessionInfoDTO sessionInfo;
    private PromptInfoDTO promptInfo;
    private CallInfoDTO callInfo;
    private ResponseInfoDTO responseInfo;
    private TestRunInfoDTO testRunInfo;
    private Map<String, Object> evalResults;
    private TraceInfoDTO traceInfo;

    public RecordDTO() {
    }

    public RecordDTO(
            List<ChatMessage> messages,
            Map<String, Object> inputs,
            SessionInfoDTO sessionInfo,
            PromptInfoDTO promptInfo,
            CallInfoDTO callInfo,
            ResponseInfoDTO responseInfo,
            TestRunInfoDTO testRunInfo,
            Map<String, Object> evalResults,
            TraceInfoDTO traceInfo
    ) {
        this.messages = messages;
        this.inputs = inputs;
        this.sessionInfo = sessionInfo;
        this.promptInfo = promptInfo;
        this.callInfo = callInfo;
        this.responseInfo = responseInfo;
        this.testRunInfo = testRunInfo;
        this.evalResults = evalResults;
        this.traceInfo = traceInfo;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public SessionInfoDTO getSessionInfo() {
        return sessionInfo;
    }

    public PromptInfoDTO getPromptInfo() {
        return promptInfo;
    }

    public CallInfoDTO getCallInfo() {
        return callInfo;
    }

    public ResponseInfoDTO getResponseInfo() {
        return responseInfo;
    }

    public TestRunInfoDTO getTestRunInfo() {
        return testRunInfo;
    }
    public TraceInfoDTO getTraceInfo(){return traceInfo;}

    @Override
    public String toString() {
        return "RecordDTO{" +
                "messages=" + messages +
                ", inputs=" + inputs +
                ", sessionInfo=" + sessionInfo +
                ", promptInfo=" + promptInfo +
                ", callInfo=" + callInfo +
                ", responseInfo=" + responseInfo +
                ", testRunInfo=" + testRunInfo +
                ", evalResults=" + evalResults +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordDTO recordDTO = (RecordDTO) o;
        return Objects.equals(messages, recordDTO.messages) && Objects.equals(inputs, recordDTO.inputs) && Objects.equals(sessionInfo, recordDTO.sessionInfo) && Objects.equals(promptInfo, recordDTO.promptInfo) && Objects.equals(callInfo, recordDTO.callInfo) && Objects.equals(responseInfo, recordDTO.responseInfo) && Objects.equals(testRunInfo, recordDTO.testRunInfo) && Objects.equals(evalResults, recordDTO.evalResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages, inputs, sessionInfo, promptInfo, callInfo, responseInfo, testRunInfo, evalResults);
    }

    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SessionInfoDTO {
        private String sessionId;
        private Map<String, Object> customMetadata;

        public SessionInfoDTO() {
        }

        public SessionInfoDTO(String sessionId, Map<String, Object> customMetadata) {
            this.sessionId = sessionId;
            this.customMetadata = customMetadata;
        }

        public String getSessionId() {
            return sessionId;
        }
        public Map<String, Object> getCustomMetadata() {
            return customMetadata;
        }

        @Override
        public String toString() {
            return "SessionInfoDTO{" +
                    "sessionId=" + sessionId +
                    ", customMetadata=" + customMetadata +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SessionInfoDTO sessionInfoDTO = (SessionInfoDTO) o;
            return Objects.equals(sessionId, sessionInfoDTO.sessionId) && Objects.equals(customMetadata, sessionInfoDTO.customMetadata);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, customMetadata);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PromptInfoDTO {
        private String promptTemplateId;
        private String promptTemplateVersionId;
        private String templateName;
        private String environment;
        private Map<String, Object> modelParameters;
        private Map<String, Object> providerInfo;
        private String provider;
        private String model;
        private String flavorName;
        private String projectId;

        public PromptInfoDTO() {
        }

        public PromptInfoDTO(
                String promptTemplateId,
                String promptTemplateVersionId,
                String templateName,
                String environment,
                Map<String, Object> modelParameters,
                Map<String, Object> providerInfo,
                String provider,
                String model,
                String flavorName,
                String projectId
        ) {
            this.promptTemplateId = promptTemplateId;
            this.promptTemplateVersionId = promptTemplateVersionId;
            this.templateName = templateName;
            this.environment = environment;
            this.modelParameters = modelParameters;
            this.providerInfo = providerInfo;
            this.provider = provider;
            this.model = model;
            this.flavorName = flavorName;
            this.projectId = projectId;
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
        public String getProjectId() { return projectId; }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PromptInfoDTO that = (PromptInfoDTO) o;
            return Objects.equals(promptTemplateId, that.promptTemplateId) && Objects.equals(promptTemplateVersionId, that.promptTemplateVersionId) && Objects.equals(templateName, that.templateName) && Objects.equals(environment, that.environment) && Objects.equals(modelParameters, that.modelParameters)  && Objects.equals(providerInfo, that.providerInfo) && Objects.equals(provider, that.provider) && Objects.equals(model, that.model) && Objects.equals(flavorName, that.flavorName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(promptTemplateId, promptTemplateVersionId, templateName, environment, modelParameters, providerInfo, provider, model, flavorName);
        }

        @Override
        public String toString() {
            return "PromptInfo{" +
                    "promptTemplateId='" + promptTemplateId + '\'' +
                    ", promptTemplateVersionId='" + promptTemplateVersionId + '\'' +
                    ", templateName='" + templateName + '\'' +
                    ", environment='" + environment + '\'' +
                    ", modelParameters=" + modelParameters +
                    ", providerInfo=" + providerInfo +
                    ", provider='" + provider + '\'' +
                    ", model='" + model + '\'' +
                    ", flavorName='" + flavorName + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CallInfoDTO {
        private String provider;
        private String model;
        private double startTime;
        private double endTime;
        private Map<String, Object> modelParameters;
        private Map<String, Object> providerInfo;

        public CallInfoDTO() {
        }

        public CallInfoDTO(
                String provider,
                String model,
                long startTime,
                long endTime,
                Map<String, Object> modelParameters
        ) {
            this.provider = provider;
            this.model = model;
            this.startTime = startTime;
            this.endTime = endTime;
            this.modelParameters = modelParameters;
        }

        public CallInfoDTO providerInfo(Map<String, Object> providerInfo) {
            this.providerInfo = providerInfo;
            return this;
        }

        public String getProvider() {
            return provider;
        }

        public String getModel() {
            return model;
        }

        public double getStartTime() {
            return this.startTime;
        }

        public double getEndTime() {
            return this.endTime;
        }

        public Map<String, Object> getModelParameters() {
            return modelParameters;
        }

        public Map<String, Object> getProviderInfo() {
            return providerInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CallInfoDTO that = (CallInfoDTO) o;
            return Objects.equals(provider, that.provider) && Objects.equals(model, that.model) && Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime) && Objects.equals(modelParameters, that.modelParameters)  && Objects.equals(providerInfo, that.providerInfo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, model, startTime, endTime, modelParameters, providerInfo);
        }

        @Override
        public String toString() {
            return "CallInfoDTO{" +
                    "provider='" + provider + '\'' +
                    ", model='" + model + '\'' +
                    ", startTime='" + startTime + '\'' +
                    ", endTime='" + endTime + '\'' +
                    ", modelParameters=" + modelParameters +
                    ", providerInfo=" + providerInfo +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ResponseInfoDTO {
        private boolean isComplete;
        private OpenAIFunctionCallDTO functionCall;
        private int promptTokens;
        private int responseTokens;

        public ResponseInfoDTO() {
        }

        public ResponseInfoDTO(boolean isComplete) {
            this.isComplete = isComplete;
        }

        public ResponseInfoDTO(
                boolean isComplete,
                OpenAIFunctionCallDTO functionCall,
                int promptTokens,
                int responseTokens
        ) {
            this.isComplete = isComplete;
            this.functionCall = functionCall;
            this.promptTokens = promptTokens;
            this.responseTokens = responseTokens;
        }

        public ResponseInfoDTO functionCall(OpenAIFunctionCallDTO functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public ResponseInfoDTO promptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public ResponseInfoDTO responseTokens(int responseTokens) {
            this.responseTokens = responseTokens;
            return this;
        }

        public boolean getIsComplete() {
            return isComplete;
        }

        public OpenAIFunctionCallDTO getFunctionCall() {
            return functionCall;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public int getResponseTokens() {
            return responseTokens;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResponseInfoDTO that = (ResponseInfoDTO) o;
            return Objects.equals(isComplete, that.isComplete) && Objects.equals(functionCall, that.functionCall) && Objects.equals(promptTokens, that.promptTokens) && Objects.equals(responseTokens, that.responseTokens);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isComplete, functionCall, promptTokens, responseTokens);
        }

        @Override
        public String toString() {
            return "ResponseInfoDTO{" +
                    "isComplete='" + isComplete + '\'' +
                    ", functionCall='" + functionCall + '\'' +
                    ", promptTokens='" + promptTokens + '\'' +
                    ", responseTokens='" + responseTokens + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public static class TestRunInfoDTO {
        private String testRunId;
        private String testCaseId;

        public TestRunInfoDTO() {
        }

        public TestRunInfoDTO(String testRunId, String testCaseId) {
            this.testRunId = testRunId;
            this.testCaseId = testCaseId;
        }

        public String getTestRunId() {
            return testRunId;
        }

        public String getTestCaseId() {
            return testCaseId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestRunInfoDTO that = (TestRunInfoDTO) o;
            return Objects.equals(testRunId, that.testRunId) && Objects.equals(testCaseId, that.testCaseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testRunId, testCaseId);
        }

        @Override
        public String toString() {
            return "TestRunInfoDTO{" +
                    "testRunId='" + testRunId + '\'' +
                    ", testCaseId='" + testCaseId + '\'' +
                    '}';
        }


    }

    public static class OpenAIFunctionCallDTO {
        private String name;
        private String arguments;

        public OpenAIFunctionCallDTO() {
        }

        public OpenAIFunctionCallDTO(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }
        public String getArguments() {
            return arguments;
        }

        public Map<String, String> asMap() {
            return Map.of(
                    "name", name,
                    "arguments", arguments
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OpenAIFunctionCallDTO that = (OpenAIFunctionCallDTO) o;
            return Objects.equals(name, that.name) && Objects.equals(arguments, that.arguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments);
        }

        @Override
        public String toString() {
            return "OpenAIFunctionCallDTO{" +
                    "name='" + name + '\'' +
                    ", arguments='" + arguments + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TraceInfoDTO {
        private UUID traceId;

        public TraceInfoDTO() {
        }

        public TraceInfoDTO(UUID traceId){
            this.traceId = traceId;
        }

        public UUID getTraceId(){return traceId;}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TraceInfoDTO that = (TraceInfoDTO) o;
            return Objects.equals(traceId, that.traceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(traceId);
        }

        @Override
        public String toString() {
            return "TraceInfoDTO{" +
                    "traceId='" + traceId + '\'' +
                    '}';
        }
    }
}
