package ai.freeplay.client.thin.resources.sessions;

import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.internal.ParameterUtils.validateBasicMap;

@JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Session {
    private final UUID sessionId;
    private Map<String, Object> customMetadata;

    public Session() {
        sessionId = UUID.randomUUID();
    }

    @SuppressWarnings("unused")
    public Session(
            UUID sessionId
    ) {
        this.sessionId = sessionId;
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
