package ai.freeplay.client.thin.resources.sessions;

import ai.freeplay.client.thin.internal.ThinCallSupport;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.internal.ParameterUtils.validateBasicMap;

@JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Session {
    private final UUID sessionId;
    private Map<String, Object> customMetadata;
    private final ThinCallSupport callSupport;

    public Session(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
        sessionId = UUID.randomUUID();
    }

    // this can't be used by customer to restore sessions anynmore?
    public Session(
            UUID sessionId, ThinCallSupport callSupport
    ) {
        this.sessionId = sessionId;
        this.callSupport = callSupport;
    }

    public Session customMetadata(Map<String, Object> customMetadata) {
        validateBasicMap(customMetadata);
        this.customMetadata = customMetadata;
        return this;
    }

    public String getSessionId() {
        return sessionId.toString();
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    public SessionInfo getSessionInfo() {
        return new SessionInfo(sessionId.toString(), customMetadata);
    }

    public TraceInfo createTrace(String input) {
        return new TraceInfo(
                this.sessionId,
                UUID.randomUUID(),
                input,
                this.callSupport
        );
    }

    public TraceInfo createTrace(CreateTracePayload payload) {
        TraceInfo trace = new TraceInfo(
                this.sessionId,
                UUID.randomUUID(),
                payload.getInput(),
                this.callSupport,
                payload.getKind(),
                payload.getName(),
                null
        );
        if (payload.getAgentName() != null) {
            trace.agentName(payload.getAgentName());
        }
        if (payload.getParentId() != null) {
            trace.parentId(payload.getParentId());
        }
        if (payload.getCustomMetadata() != null) {
            trace.customMetadata(payload.getCustomMetadata());
        }
        return trace;
    }

    /**
     * @deprecated use {@link #createTrace(String)} instead and use the builder pattern to set fields on TraceInfo.
     * For example:
     * ```java
     * TraceInfo traceInfo = session.createTrace("input")
     *     .agentName("agentName")
     *     .parentId(parentId)
     *     .customMetadata(customMetadata);
     * ```
     */
    @Deprecated
    public TraceInfo createTrace(String input, String agentName, Map<String, Object> customMetadata) {
        return new TraceInfo(
                this.sessionId,
                UUID.randomUUID(),
                input,
                this.callSupport
        ).agentName(agentName).customMetadata(customMetadata);
    }
}
