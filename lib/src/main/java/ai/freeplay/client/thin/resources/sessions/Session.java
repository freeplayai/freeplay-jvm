package ai.freeplay.client.thin.resources.sessions;

import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.internal.ParameterUtils.validateBasicMap;

public class Session {
    private final UUID sessionId;
    private Map<String, Object> customMetadata;

    public Session() {
        sessionId = UUID.randomUUID();
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
}
