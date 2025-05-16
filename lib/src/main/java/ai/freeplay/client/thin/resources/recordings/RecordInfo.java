package ai.freeplay.client.thin.resources.recordings;

import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.thin.GeminiLLMAdapter;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.PromptInfo;
import ai.freeplay.client.thin.resources.sessions.SessionInfo;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;
import com.google.cloud.vertexai.api.Content;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RecordInfo {
    private final List<ChatMessage> allMessages;
    private final Map<String, Object> inputs;
    private final SessionInfo sessionInfo;

    private final PromptInfo promptInfo;
    private final CallInfo callInfo;
    private final ResponseInfo responseInfo;
    private TestRunInfo testRunInfo;
    private Map<String, Object> evalResults;

    private TraceInfo traceInfo;
    private UUID completionId;

    private List<Map<String, Object>> toolSchema;
    private MediaInputCollection mediaInputCollection;

    public RecordInfo(
            List<ChatMessage> allMessages,
            Map<String, Object> inputs,
            SessionInfo sessionInfo,
            PromptInfo promptInfo,
            CallInfo callInfo,
            ResponseInfo responseInfo
    ) {
        this.allMessages = allMessages;
        this.inputs = inputs;
        this.sessionInfo = sessionInfo;
        this.promptInfo = promptInfo;
        this.callInfo = callInfo;
        this.responseInfo = responseInfo;
    }

    public static RecordInfo fromGeminiContent(
            List<Content> contents,
            Map<String, Object> inputs,
            SessionInfo sessionInfo,
            PromptInfo promptInfo,
            CallInfo callInfo,
            ResponseInfo responseInfo
    ) {
        List<ChatMessage> messages = contents.stream()
                .map(GeminiLLMAdapter::chatMessageFromContent)
                .collect(Collectors.toList());

        return new RecordInfo(messages, inputs, sessionInfo, promptInfo, callInfo, responseInfo);
    }

    public RecordInfo testRunInfo(TestRunInfo testRunInfo) {
        this.testRunInfo = testRunInfo;
        return this;
    }

    public RecordInfo evalResults(Map<String, Object> evalResults) {
        this.evalResults = evalResults;
        return this;
    }

    public RecordInfo traceInfo(TraceInfo traceInfo) {
        this.traceInfo = traceInfo;
        return this;
    }

    public RecordInfo toolSchema(List<Map<String, Object>> toolSchema) {
        this.toolSchema = toolSchema;
        return this;
    }

    public RecordInfo completionId(UUID id) {
        this.completionId = id;
        return this;
    }

    public RecordInfo mediaInputCollection(MediaInputCollection mediaInputCollection) {
        this.mediaInputCollection = mediaInputCollection;
        return this;
    }

    public List<ChatMessage> getAllMessages() {
        return allMessages;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public CallInfo getCallInfo() {
        return callInfo;
    }

    public ResponseInfo getResponseInfo() {
        return responseInfo;
    }

    public TestRunInfo getTestRunInfo() {
        return testRunInfo;
    }

    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    public TraceInfo getTraceInfo() {
        return traceInfo;
    }

    public List<Map<String, Object>> getToolSchema() {
        return toolSchema;
    }

    public UUID getCompletionId() {
        return completionId;
    }

    public MediaInputCollection getMediaInputCollection() {
        return mediaInputCollection;
    }

    @Override
    public String toString() {
        return "RecordInfo{" +
                "allMessages=" + allMessages +
                ", completionId=" + completionId +
                ", inputs=" + inputs +
                ", sessionInfo=" + sessionInfo +
                ", promptInfo=" + promptInfo +
                ", callInfo=" + callInfo +
                ", responseInfo=" + responseInfo +
                ", testRunInfo=" + testRunInfo +
                ", evalResults=" + evalResults +
                ", traceInfo=" + traceInfo +
                ", toolSchema=" + toolSchema +
                ", mediaInputCollection=" + mediaInputCollection +
                '}';
    }
}
