package ai.freeplay.client.thin;


import java.util.List;
import java.util.Map;

public class RecordPayload {
    private final List<ChatMessage> allMessages;
    private final Map<String, Object> inputs;
    private final String sessionId;

    private final PromptInfo promptInfo;
    private final CallInfo callInfo;
    private final ResponseInfo responseInfo;
    private final TestRunInfo testRunInfo;

    public RecordPayload(
            List<ChatMessage> allMessages,
            Map<String, Object> inputs,
            String sessionId,
            PromptInfo promptInfo,
            CallInfo callInfo,
            ResponseInfo responseInfo,
            TestRunInfo testRunInfo
    ) {
        this.allMessages = allMessages;
        this.inputs = inputs;
        this.sessionId = sessionId;
        this.promptInfo = promptInfo;
        this.callInfo = callInfo;
        this.responseInfo = responseInfo;
        this.testRunInfo = testRunInfo;
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
