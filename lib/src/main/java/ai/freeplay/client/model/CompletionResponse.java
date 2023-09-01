package ai.freeplay.client.model;

public class CompletionResponse {
    private final String content;
    private final boolean isComplete;
    private final boolean isLast;

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
}
