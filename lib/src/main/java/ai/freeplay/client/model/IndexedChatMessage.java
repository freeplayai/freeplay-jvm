package ai.freeplay.client.model;

public class IndexedChatMessage extends ChatMessage {
    private final boolean isLast;
    private final int index;
    private final boolean isComplete;
    private String completionId;

    public IndexedChatMessage(String role, String content, int index, boolean isComplete) {
        this(role, content, index, isComplete, false);
    }

    public IndexedChatMessage(String role, String content, int index, boolean isComplete, boolean isLast) {
        super(role, content);
        this.index = index;
        this.isComplete = isComplete;
        this.isLast = isLast;
    }

    @SuppressWarnings("unused")
    public int getIndex() {
        return index;
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
