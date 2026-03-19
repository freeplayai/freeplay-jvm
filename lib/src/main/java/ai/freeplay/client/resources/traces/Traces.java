package ai.freeplay.client.resources.traces;

import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.CallSupport;

import java.util.concurrent.CompletableFuture;

public class Traces {

    private final CallSupport callSupport;

    public Traces(CallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<TraceUpdateResponse> update(TraceUpdatePayload payload) {
        if (payload.getOutput() == null && payload.getEvalResults() == null) {
            throw new FreeplayClientException(
                    "At least one of 'output' or 'evalResults' must be provided"
            );
        }
        return callSupport.updateTrace(
                payload.getProjectId(),
                payload.getSessionId(),
                payload.getTraceId(),
                payload.getOutput(),
                payload.getEvalResults()
        );
    }
}
