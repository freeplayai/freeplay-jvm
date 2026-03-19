package ai.freeplay.client.resources.traces;

import java.util.Map;

public class TraceUpdatePayload {
    private final String projectId;
    private final String sessionId;
    private final String traceId;
    private Object output;
    private Map<String, Object> evalResults;

    public TraceUpdatePayload(String projectId, String sessionId, String traceId) {
        this.projectId = projectId;
        this.sessionId = sessionId;
        this.traceId = traceId;
    }

    public TraceUpdatePayload output(Object output) {
        this.output = output;
        return this;
    }

    public TraceUpdatePayload evalResults(Map<String, Object> evalResults) {
        this.evalResults = evalResults;
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

    public Map<String, Object> getEvalResults() {
        return evalResults;
    }
}
