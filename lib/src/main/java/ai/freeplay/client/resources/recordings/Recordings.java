package ai.freeplay.client.resources.recordings;

import ai.freeplay.client.internal.CallSupport;

import java.util.concurrent.CompletableFuture;

public class Recordings {
    private final CallSupport callSupport;

    public Recordings(CallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<RecordResponse> create(RecordPayload recordPayload) {
        return callSupport.record(recordPayload);
    }
}
