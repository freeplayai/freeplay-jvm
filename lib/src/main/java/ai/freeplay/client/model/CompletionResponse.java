package ai.freeplay.client.model;

public class CompletionResponse {
    private final String content;
    private final boolean isComplete;
    private final boolean isLast;
    private String completionId;

    public CompletionResponse(String content, boolean isComplete, boolean isLast) {
        this.content = content;
        this.isComplete = isComplete;
        this.isLast = isLast;
    }

    public String getContent() {
        return content;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean isLast() {
        return isLast;
    }

    public String getCompletionId() {
        return completionId;
    }

    public void setCompletionId(String completionId) {
        this.completionId = completionId;
    }
}
