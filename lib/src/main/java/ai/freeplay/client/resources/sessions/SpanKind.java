package ai.freeplay.client.resources.sessions;

public enum SpanKind {
    TOOL("tool"),
    AGENT("agent");

    private final String value;

    SpanKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

