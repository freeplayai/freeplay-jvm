package ai.freeplay.client.thin.resources.sessions;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.Objects;
import java.util.UUID;

public class TraceInfo {
    public UUID sessionId;
    public UUID traceId;
    public String input;
    public String output;
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

    public UUID getSessionId() {
        return sessionId;
    }
    public UUID getTraceId() {
        return traceId;
    }
    public String getInput() {
        return input;
    }
    public String getOutput(){
        return output;
    }

    public void recordOutput(String projectId, String output){
        this.output = output;
        callSupport.recordTrace(projectId, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfo that = (TraceInfo) o;
        return Objects.equals(traceId, that.traceId) && Objects.equals(sessionId, that.sessionId) && Objects.equals(input, that.input) && Objects.equals(output, that.output);
    }



}
