package ai.freeplay.client.resources.sessions;

import ai.freeplay.client.internal.CallSupport;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Sessions {

    private final CallSupport callSupport;

    public Sessions(CallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public Session create() {
        return new Session(this.callSupport);
    }

    @SuppressWarnings("unused")
    public Session restore(UUID sessionId) {
        return new Session(sessionId, callSupport);
    }

    public CompletableFuture<SessionDeleteResponse> delete(String projectId, String sessionId) {
        return callSupport.deleteSession(projectId, sessionId);
    }
}
