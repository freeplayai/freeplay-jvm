package ai.freeplay.client.internal;

import java.time.Instant;
import java.util.Map;

public class CallInfo {
    private final String sessionId;
    private final String testRunId;
    private final double startTime;
    private final double endTime;
    private final String tag;
    private final Map<String, Object> inputs;
    private final String promptContent;
    private final String returnContent;
    private final boolean isComplete;

    private static double instantToDouble(Instant instant) {
        return Double.parseDouble(instant.getEpochSecond() + "." + instant.getNano());
    }

    public CallInfo(
            String sessionId,
            String testRunId,
            Instant startTime,
            Instant endTime,
            String tag,
            Map<String, Object> inputs,
            String promptContent,
            String returnContent,
            boolean isComplete) {
        this.sessionId = sessionId;
        this.testRunId = testRunId;
        this.startTime = instantToDouble(startTime);
        this.endTime = instantToDouble(endTime);
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
