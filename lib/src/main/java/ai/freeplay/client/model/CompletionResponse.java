package ai.freeplay.client.model;

public class CompletionResponse {
    private final String content;
    private final boolean isComplete;

    public CompletionResponse(String content, boolean isComplete) {
        this.content = content;
        this.isComplete = isComplete;
    }

    public String getContent() {
        return content;
    }

    public boolean isComplete() {
        return isComplete;
    }
}
