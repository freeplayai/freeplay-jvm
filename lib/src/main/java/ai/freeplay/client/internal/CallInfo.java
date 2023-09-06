package ai.freeplay.client.internal;

import java.util.Map;

public class CallInfo {
    private final String sessionId;
    private final String testRunId;
    private final float startTime;
    private final float endTime;
    private final String tag;
    private final Map<String, Object> inputs;
    private final String promptContent;
    private final String returnContent;
    private final boolean isComplete;

    public CallInfo(
            String sessionId,
            String testRunId,
            float startTime,
            float endTime,
            String tag,
            Map<String, Object> inputs,
            String promptContent,
            String returnContent,
            boolean isComplete) {
        this.sessionId = sessionId;
        this.testRunId = testRunId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tag = tag;
        this.inputs = inputs;
        this.promptContent = promptContent;
        this.returnContent = returnContent;
        this.isComplete = isComplete;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public float getStartTime() {
        return startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    public String getTag() {
        return tag;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public String getPromptContent() {
        return promptContent;
    }

    public String getReturnContent() {
        return returnContent;
    }

    public boolean isComplete() {
        return isComplete;
    }
}
