package ai.freeplay.client.thin.resources.recordings;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.concurrent.CompletableFuture;

public class Recordings {
    private final ThinCallSupport callSupport;

    public Recordings(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<RecordResponse> create(RecordInfo recordPayload) {
        return callSupport.record(recordPayload);
    }
}
