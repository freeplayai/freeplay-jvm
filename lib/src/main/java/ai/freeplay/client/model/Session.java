package ai.freeplay.client.model;

public class Session {
    private final String sessionId;

    public Session(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
