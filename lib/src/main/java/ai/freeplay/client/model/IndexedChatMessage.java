package ai.freeplay.client.model;

public class IndexedChatMessage extends ChatMessage {
    private final int index;
    private final boolean isComplete;

    public IndexedChatMessage(String role, String content, int index, boolean isComplete) {
        super(role, content);
        this.index = index;
        this.isComplete = isComplete;
    }

    @SuppressWarnings("unused")
    public int getIndex() {
        return index;
    }

    public boolean isComplete() {
        return isComplete;
    }
}
