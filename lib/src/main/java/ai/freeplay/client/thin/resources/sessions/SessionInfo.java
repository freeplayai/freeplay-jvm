package ai.freeplay.client.thin.resources.sessions;

import java.util.Map;

public class SessionInfo {
    private final String sessionId;
    private final Map<String, Object> customMetadata;

    public SessionInfo(String sessionId, Map<String, Object> customMetadata) {
        this.sessionId = sessionId;
        this.customMetadata = customMetadata;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    @Override
    public String toString() {
        return "SessionInfo{" +
                "sessionId=" + sessionId +
                ", customMetadata=" + customMetadata +
                '}';
    }
}
