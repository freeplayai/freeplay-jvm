package ai.freeplay.client.resources.recordings;

import ai.freeplay.client.adapters.GeminiLLMAdapter;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.PromptVersionInfo;
import ai.freeplay.client.resources.sessions.SessionInfo;
import com.google.cloud.vertexai.api.Content;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RecordPayload {
    private final String projectId;
    private final List<ChatMessage> allMessages;
    private Map<String, Object> inputs;
    private SessionInfo sessionInfo;

    private PromptVersionInfo promptVersionInfo;
    private CallInfo callInfo;
    private ResponseInfo responseInfo;
    private TestRunInfo testRunInfo;
    private Map<String, Object> evalResults;

    private UUID parentId;
    private UUID completionId;

    private List<Map<String, Object>> toolSchema;
    private Map<String, Object> outputSchema;
    private MediaInputCollection mediaInputCollection;

    public RecordPayload(
            String projectId,
            List<ChatMessage> allMessages
    ) {
        this.projectId = projectId;
        this.allMessages = allMessages;
        this.sessionInfo = new SessionInfo(UUID.randomUUID().toString(), null);
        this.inputs = null;
        this.promptVersionInfo = null;
        this.callInfo = null;
        this.responseInfo = null;
    }

    // Builder-style constructor for optional fields
    private RecordPayload(
            String projectId,
            List<ChatMessage> allMessages,
            Map<String, Object> inputs,
            SessionInfo sessionInfo,
            PromptVersionInfo promptVersionInfo,
            CallInfo callInfo,
            ResponseInfo responseInfo
    ) {
        this.projectId = projectId;
        this.allMessages = allMessages;
        this.inputs = inputs;
        this.sessionInfo = sessionInfo;
        this.promptVersionInfo = promptVersionInfo;
        this.callInfo = callInfo;
        this.responseInfo = responseInfo;
    }

    public static RecordPayload fromGeminiContent(
            String projectId,
            List<Content> contents,
            Map<String, Object> inputs,
            SessionInfo sessionInfo,
            PromptVersionInfo promptVersionInfo,
            CallInfo callInfo,
            ResponseInfo responseInfo
    ) {
        List<ChatMessage> messages = contents.stream()
                .map(GeminiLLMAdapter::chatMessageFromContent)
                .collect(Collectors.toList());

        return new RecordPayload(projectId, messages, inputs, sessionInfo, promptVersionInfo, callInfo, responseInfo);
    }

    public RecordPayload sessionInfo(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
        return this;
    }

    public RecordPayload inputs(Map<String, Object> inputs) {
        this.inputs = inputs;
        return this;
    }

    public RecordPayload promptVersionInfo(PromptVersionInfo promptVersionInfo) {
        this.promptVersionInfo = promptVersionInfo;
        return this;
    }

    public RecordPayload callInfo(CallInfo callInfo) {
        this.callInfo = callInfo;
        return this;
    }

    public RecordPayload responseInfo(ResponseInfo responseInfo) {
        this.responseInfo = responseInfo;
        return this;
    }

    public RecordPayload testRunInfo(TestRunInfo testRunInfo) {
        this.testRunInfo = testRunInfo;
        return this;
    }

    public RecordPayload evalResults(Map<String, Object> evalResults) {
        this.evalResults = evalResults;
        return this;
    }

    public RecordPayload parentId(UUID parentId) {
        this.parentId = parentId;
        return this;
    }

    public RecordPayload toolSchema(List<Map<String, Object>> toolSchema) {
        this.toolSchema = toolSchema;
        return this;
    }

    public RecordPayload outputSchema(Map<String, Object> outputSchema) {
        this.outputSchema = outputSchema;
        return this;
    }

    public RecordPayload completionId(UUID id) {
        this.completionId = id;
        return this;
    }

    public RecordPayload mediaInputCollection(MediaInputCollection mediaInputCollection) {
        this.mediaInputCollection = mediaInputCollection;
        return this;
    }

    public List<ChatMessage> getAllMessages() {
        return allMessages;
    }

    public String getProjectId() {
        return projectId;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public PromptVersionInfo getPromptVersionInfo() {
        return promptVersionInfo;
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

    public UUID getParentId() {
        return parentId;
    }

    public List<Map<String, Object>> getToolSchema() {
        return toolSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public UUID getCompletionId() {
        return completionId;
    }

    public MediaInputCollection getMediaInputCollection() {
        return mediaInputCollection;
    }

    @Override
    public String toString() {
        return "RecordPayload{" +
                "projectId='" + projectId + '\'' +
                ", allMessages=" + allMessages +
                ", completionId=" + completionId +
                ", inputs=" + inputs +
                ", sessionInfo=" + sessionInfo +
                ", promptVersionInfo=" + promptVersionInfo +
                ", callInfo=" + callInfo +
                ", responseInfo=" + responseInfo +
                ", testRunInfo=" + testRunInfo +
                ", evalResults=" + evalResults +
                ", parentId=" + parentId +
                ", toolSchema=" + toolSchema +
                ", outputSchema=" + outputSchema +
                ", mediaInputCollection=" + mediaInputCollection +
                '}';
    }
}
