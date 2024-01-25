package ai.freeplay.client.thin.resources.sessions;

import java.util.UUID;

public class Session {
    private final UUID sessionId;

    public Session() {
        sessionId = UUID.randomUUID();
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
