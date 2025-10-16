package ai.freeplay.client.thin.resources.sessions;

import java.util.Map;
import java.util.UUID;

public class CreateTracePayload {
    private Object input;
    private SpanKind kind;
    private String agentName;
    private UUID parentId;
    private String name;
    private Map<String, Object> customMetadata;

    public CreateTracePayload(Object input) {
        this.input = input;
    }

    public CreateTracePayload kind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    public CreateTracePayload agentName(String agentName) {
        this.agentName = agentName;
        return this;
    }

    public CreateTracePayload parentId(UUID parentId) {
        this.parentId = parentId;
        return this;
    }

    public CreateTracePayload name(String name) {
        this.name = name;
        return this;
    }

    public CreateTracePayload customMetadata(Map<String, Object> customMetadata) {
        this.customMetadata = customMetadata;
        return this;
    }

    public Object getInput() {
        return input;
    }

    public SpanKind getKind() {
        return kind;
    }

    public String getAgentName() {
        return agentName;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }
}

