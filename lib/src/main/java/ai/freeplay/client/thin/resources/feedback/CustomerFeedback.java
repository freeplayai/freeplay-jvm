package ai.freeplay.client.thin.resources.feedback;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CustomerFeedback {

    private final ThinCallSupport callSupport;

    public CustomerFeedback(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    public CompletableFuture<CustomerFeedbackResponse> update(String projectId, String completionId, Map<String, Object> feedback) {
        return callSupport.updateCustomerFeedback(projectId, completionId, feedback);
    }

    public CompletableFuture<TraceFeedbackResponse> updateTrace(String projectId, String traceId, Map<String, Object> feedback) {
        return callSupport.updateTraceFeedback(projectId, traceId, feedback);
    }
}
