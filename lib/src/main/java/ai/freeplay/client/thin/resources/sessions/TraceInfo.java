package ai.freeplay.client.thin.resources.sessions;

import ai.freeplay.client.thin.internal.ThinCallSupport;
import ai.freeplay.client.thin.resources.recordings.TestRunInfo;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TraceInfo {
    public UUID sessionId;
    public UUID traceId;
    public Object input;
    public Object output;

    public String agentName;
    public UUID parentId;
    public SpanKind kind;
    public String name;
    public Instant startTime;

    public Map<String, Object> customMetadata;

    private Map<String, Object> evalResults;
    private Instant endTime;

    private final ThinCallSupport callSupport;

    public TraceInfo(
            UUID sessionId,
            UUID traceId,
            Object input,
            ThinCallSupport callSupport,
            SpanKind kind,
            String name,
            Instant startTime
    ) {
        this.sessionId = sessionId;
        this.traceId = traceId;
        this.input = input;
        this.output = null;
        this.callSupport = callSupport;
        this.kind = kind;
        this.name = name;
        this.startTime = startTime != null ? startTime : Instant.now();
    }

    public TraceInfo(
            UUID sessionId,
            UUID traceId,
            Object input,
            ThinCallSupport callSupport
    ) {
        this(sessionId, traceId, input, callSupport, null, null, null);
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

    public TraceInfo parentId(
            UUID parentId
    ) {
        this.parentId = parentId;
        return this;
    }

    public TraceInfo kind(
            SpanKind kind
    ) {
        this.kind = kind;
        return this;
    }

    public TraceInfo name(
            String name
    ) {
        this.name = name;
        return this;
    }


    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public Object getInput() {
        return input;
    }

    public Object getOutput() {
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

    public UUID getParentId() {
        return parentId;
    }

    public SpanKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    @SuppressWarnings("unused")
    public CompletableFuture<TraceRecordResponse> recordOutput(String projectId, Object output) {
        this.output = output;
        this.endTime = Instant.now();
        return callSupport.recordTrace(projectId, this);
    }

    public CompletableFuture<TraceRecordResponse> recordOutput(String projectId, Object output, Map<String, Object> evalResults) {
        this.output = output;
        this.evalResults = evalResults;
        this.endTime = Instant.now();
        return callSupport.recordTrace(projectId, this);
    }

    public CompletableFuture<TraceRecordResponse> recordOutput(String projectId, Object output, Map<String, Object> evalResults, TestRunInfo testRunInfo) {
        this.output = output;
        this.evalResults = evalResults;
        this.endTime = Instant.now();
        return callSupport.recordTrace(projectId, this, testRunInfo);
    }

    public CompletableFuture<TraceRecordResponse> recordOutput(String projectId, Object output, Map<String, Object> evalResults, TestRunInfo testRunInfo, Instant endTime) {
        this.output = output;
        this.evalResults = evalResults;
        this.endTime = endTime != null ? endTime : Instant.now();
        return callSupport.recordTrace(projectId, this, testRunInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfo that = (TraceInfo) o;
        return Objects.equals(traceId, that.traceId) && Objects.equals(sessionId, that.sessionId) && Objects.equals(input, that.input) && Objects.equals(output, that.output);
    }


}
