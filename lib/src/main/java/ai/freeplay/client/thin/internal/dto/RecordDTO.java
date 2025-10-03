package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.media.MediaInput;
import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputUrl;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
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
    @JsonProperty("prompt_info")
    private PromptVersionInfoDTO promptVersionInfo;
    private CallInfoDTO callInfo;
    private ResponseInfoDTO responseInfo;
    private TestRunInfoDTO testRunInfo;
    private Map<String, Object> evalResults;
    private TraceInfoDTO traceInfo;
    private UUID parentId;
    private List<Map<String, Object>> toolSchema;
    private Map<String, Object> outputSchema;
    private UUID completionId;
    private Map<String, MediaInputDTO> mediaInputs;

    public RecordDTO() {
    }

    public RecordDTO(
            List<ChatMessage> messages,
            Map<String, Object> inputs,
            SessionInfoDTO sessionInfo,
            PromptVersionInfoDTO promptVersionInfo,
            CallInfoDTO callInfo,
            ResponseInfoDTO responseInfo,
            TestRunInfoDTO testRunInfo,
            Map<String, Object> evalResults,
            TraceInfoDTO traceInfo,
            UUID parentId,
            List<Map<String, Object>> toolSchema,
            Map<String, Object> outputSchema,
            UUID completionId,
            Map<String, MediaInputDTO> mediaInputs
    ) {
        this.messages = messages;
        this.inputs = inputs;
        this.sessionInfo = sessionInfo;
        this.promptVersionInfo = promptVersionInfo;
        this.callInfo = callInfo;
        this.responseInfo = responseInfo;
        this.testRunInfo = testRunInfo;
        this.evalResults = evalResults;
        this.traceInfo = traceInfo;
        this.parentId = parentId;
        this.toolSchema = toolSchema;
        this.outputSchema = outputSchema;
        this.completionId = completionId;
        this.mediaInputs = mediaInputs;
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

    public PromptVersionInfoDTO getPromptVersionInfo() {
        return promptVersionInfo;
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

    public TraceInfoDTO getTraceInfo() {
        return traceInfo;
    }

    public UUID getParentId() {
        return parentId;
    }

    public List<Map<String, Object>> getToolSchema() {
        return toolSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public Map<String, MediaInputDTO> getMediaInputs() {
        return mediaInputs;
    }

    @Override
    public String toString() {
        return "RecordDTO{" +
                "messages=" + messages +
                ", inputs=" + inputs +
                ", sessionInfo=" + sessionInfo +
                ", promptVersionInfo=" + promptVersionInfo +
                ", callInfo=" + callInfo +
                ", responseInfo=" + responseInfo +
                ", testRunInfo=" + testRunInfo +
                ", evalResults=" + evalResults +
                ", traceInfo=" + traceInfo +
                ", parentId=" + parentId +
                ", toolSchema=" + toolSchema +
                ", outputSchema=" + outputSchema +
                ", completionId=" + completionId +
                ", mediaInputs=" + mediaInputs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RecordDTO recordDTO = (RecordDTO) o;
        return Objects.equals(messages, recordDTO.messages) && Objects.equals(inputs, recordDTO.inputs) && Objects.equals(sessionInfo, recordDTO.sessionInfo) && Objects.equals(promptVersionInfo, recordDTO.promptVersionInfo) && Objects.equals(callInfo, recordDTO.callInfo) && Objects.equals(responseInfo, recordDTO.responseInfo) && Objects.equals(testRunInfo, recordDTO.testRunInfo) && Objects.equals(evalResults, recordDTO.evalResults) && Objects.equals(traceInfo, recordDTO.traceInfo) && Objects.equals(parentId, recordDTO.parentId) && Objects.equals(toolSchema, recordDTO.toolSchema) && Objects.equals(outputSchema, recordDTO.outputSchema) && Objects.equals(completionId, recordDTO.completionId) && Objects.equals(mediaInputs, recordDTO.mediaInputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages, inputs, sessionInfo, promptVersionInfo, callInfo, responseInfo, testRunInfo, evalResults, traceInfo, parentId, toolSchema, outputSchema, completionId, mediaInputs);
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
    public static class PromptVersionInfoDTO {
        private String promptTemplateVersionId;
        private String environment;
        private String projectId;

        public PromptVersionInfoDTO() {
        }

        public PromptVersionInfoDTO(
                String promptTemplateVersionId,
                String environment,
                String projectId
        ) {
            this.promptTemplateVersionId = promptTemplateVersionId;
            this.environment = environment;
            this.projectId = projectId;
        }

        public String getPromptTemplateVersionId() {
            return promptTemplateVersionId;
        }

        public String getEnvironment() {
            return environment;
        }

        public String getProjectId() {
            return projectId;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PromptVersionInfoDTO that = (PromptVersionInfoDTO) o;
            return Objects.equals(promptTemplateVersionId, that.promptTemplateVersionId) && Objects.equals(environment, that.environment) && Objects.equals(projectId, that.projectId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(promptTemplateVersionId, environment, projectId);
        }

        @Override
        public String toString() {
            return "PromptVersionInfoDTO{" +
                    "promptTemplateVersionId='" + promptTemplateVersionId + '\'' +
                    ", environment='" + environment + '\'' +
                    ", projectId='" + projectId + '\'' +
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
        private UsageTokensDTO usage;
        private ApiStyleDTO apiStyle;

        public CallInfoDTO() {
        }

        public CallInfoDTO(
                String provider,
                String model,
                double startTime,
                double endTime,
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

        public CallInfoDTO usage(UsageTokensDTO usage) {
            this.usage = usage;
            return this;
        }

        public CallInfoDTO apiStyle(CallInfo.ApiStyle apiStyle) {
            if (apiStyle == null) {
                return this;
            }
            this.apiStyle = ApiStyleDTO.valueOf(apiStyle.toString());
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

        public UsageTokensDTO getUsage() {
            return usage;
        }

        public ApiStyleDTO getApiStyle() {
            return apiStyle;
        }

        public enum ApiStyleDTO {
            BATCH("batch"), DEFAULT("default");

            private final String value;

            ApiStyleDTO(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class UsageTokensDTO {
            private final Integer completionTokens;
            private final Integer promptTokens;

            // This empty constructor is for Jackson.
            protected UsageTokensDTO() {
                this.promptTokens = null;
                this.completionTokens = null;
            }

            public UsageTokensDTO(
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

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                UsageTokensDTO that = (UsageTokensDTO) o;
                return Objects.equals(completionTokens, that.completionTokens) && Objects.equals(promptTokens, that.promptTokens);
            }

            @Override
            public int hashCode() {
                return Objects.hash(completionTokens, promptTokens);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CallInfoDTO that = (CallInfoDTO) o;
            return Double.compare(startTime, that.startTime) == 0 && Double.compare(endTime, that.endTime) == 0 && Objects.equals(provider, that.provider) && Objects.equals(model, that.model) && Objects.equals(modelParameters, that.modelParameters) && Objects.equals(providerInfo, that.providerInfo) && Objects.equals(usage, that.usage) && apiStyle == that.apiStyle;
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, model, startTime, endTime, modelParameters, providerInfo, usage, apiStyle);
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

        public TraceInfoDTO(UUID traceId) {
            this.traceId = traceId;
        }

        public UUID getTraceId() {
            return traceId;
        }

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

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaInputDTO {
        private String data;
        private String contentType;
        private String url;
        private String type;

        public MediaInputDTO(String data, String contentType) {
            this.type = "base64";
            this.data = data;
            this.contentType = contentType;
        }

        public MediaInputDTO(String url) {
            this.type = "url";
            this.url = url;
        }

        public static MediaInputDTO fromMediaInput(MediaInput input) {
            if (input instanceof MediaInputUrl) {
                MediaInputUrl url = (MediaInputUrl) input;
                return new MediaInputDTO(url.getUrl());
            } else {
                MediaInputBase64 base64 = (MediaInputBase64) input;
                return new MediaInputDTO(
                        new String(base64.getData()),
                        base64.getContentType()
                );
            }
        }

        public String getData() {
            return data;
        }

        public String getContentType() {
            return contentType;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            MediaInputDTO that = (MediaInputDTO) o;
            return Objects.equals(data, that.data) && Objects.equals(contentType, that.contentType) && Objects.equals(url, that.url) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, contentType, url, type);
        }

        @Override
        public String toString() {
            return "MediaInputDTO{" +
                    "data='" + data + '\'' +
                    ", contentType='" + contentType + '\'' +
                    ", url='" + url + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
