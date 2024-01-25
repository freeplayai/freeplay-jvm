package ai.freeplay.client.thin;

public class RecordResponse {
    private final String completionId;

    public RecordResponse(String completionId) {
        this.completionId = completionId;
    }

    public String getCompletionId() {
        return completionId;
    }
}
