package ai.freeplay.client.model;

public class ChatStart<R> {
    private final ChatSession session;
    private final R firstCompletion;

    public ChatStart(ChatSession session, R firstCompletion) {
        this.session = session;
        this.firstCompletion = firstCompletion;
    }

    public ChatSession getSession() {
        return session;
    }

    public R getFirstCompletion() {
        return firstCompletion;
    }
}
