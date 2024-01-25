package ai.freeplay.client.thin.resources.recordings;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.PromptInfo;

import java.util.List;
import java.util.Map;

public class RecordInfo {
    private final List<ChatMessage> allMessages;
    private final Map<String, Object> inputs;
    private final String sessionId;

    private final PromptInfo promptInfo;
    private final CallInfo callInfo;
    private final ResponseInfo responseInfo;
    private TestRunInfo testRunInfo;

    public RecordInfo(
            List<ChatMessage> allMessages,
            Map<String, Object> inputs,
            String sessionId,
            PromptInfo promptInfo,
            CallInfo callInfo,
            ResponseInfo responseInfo
    ) {
        this.allMessages = allMessages;
        this.inputs = inputs;
        this.sessionId = sessionId;
        this.promptInfo = promptInfo;
        this.callInfo = callInfo;
        this.responseInfo = responseInfo;
    }

    public RecordInfo testRunInfo(TestRunInfo testRunInfo) {
        this.testRunInfo = testRunInfo;
        return this;
    }

    public List<ChatMessage> getAllMessages() {
        return allMessages;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public String getSessionId() {
        return sessionId;
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
}
