package ai.freeplay.client.thin.resources.sessions;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TraceInfo {
    public UUID sessionId;
    public UUID traceId;
    public String input;
    public String output;

    public String agentName;

    public Map<String, Object> customMetadata;

    private Map<String, Object> evalResults;

    private final ThinCallSupport callSupport;

    public TraceInfo(
            UUID sessionId,
            UUID traceId,
            String input,
            ThinCallSupport callSupport
    ) {
        this.sessionId = sessionId;
        this.traceId = traceId;
        this.input = input;
        this.output = null;
        this.callSupport = callSupport;
    }

    public TraceInfo agentName(
            String agentName
    ) {
        this.agentName = agentName;
        return this;
    }

    public TraceInfo customMetadata(
            Map<String, Object> customMetadata
    ) {
        this.customMetadata = customMetadata;
        return this;
    }


    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public String getAgentName() {
        return agentName;
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    @SuppressWarnings("unused")
    public CompletableFuture<TraceRecordResponse> recordOutput(String projectId, String output) {
        this.output = output;
        return callSupport.recordTrace(projectId, this);
    }

    public CompletableFuture<TraceRecordResponse> recordOutput(String projectId, String output, Map<String, Object> evalResults) {
        this.output = output;
        this.evalResults = evalResults;
        return callSupport.recordTrace(projectId, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfo that = (TraceInfo) o;
        return Objects.equals(traceId, that.traceId) && Objects.equals(sessionId, that.sessionId) && Objects.equals(input, that.input) && Objects.equals(output, that.output);
    }


}
