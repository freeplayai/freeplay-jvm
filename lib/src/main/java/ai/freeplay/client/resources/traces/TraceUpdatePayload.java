package ai.freeplay.client.resources.traces;

import java.util.Map;
import java.util.Objects;

public class TraceUpdatePayload {
    private final String projectId;
    private final String sessionId;
    private final String traceId;
    private Object output;
    private Map<String, Object> metadata;
    private Map<String, Object> feedback;
    private Map<String, Object> evalResults;
    private String testRunId;
    private String testCaseId;

    public TraceUpdatePayload(String projectId, String sessionId, String traceId) {
        this.projectId = projectId;
        this.sessionId = sessionId;
        this.traceId = traceId;
    }

    public TraceUpdatePayload output(Object output) {
        this.output = output;
        return this;
    }

    public TraceUpdatePayload metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public TraceUpdatePayload feedback(Map<String, Object> feedback) {
        this.feedback = feedback;
        return this;
    }

    public TraceUpdatePayload evalResults(Map<String, Object> evalResults) {
        this.evalResults = evalResults;
        return this;
    }

    public TraceUpdatePayload testRunInfo(String testRunId, String testCaseId) {
        Objects.requireNonNull(testRunId, "testRunId must not be null");
        Objects.requireNonNull(testCaseId, "testCaseId must not be null");
        this.testRunId = testRunId;
        this.testCaseId = testCaseId;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Object getOutput() {
        return output;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, Object> getFeedback() {
        return feedback;
    }

    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public String getTestCaseId() {
        return testCaseId;
    }
}
