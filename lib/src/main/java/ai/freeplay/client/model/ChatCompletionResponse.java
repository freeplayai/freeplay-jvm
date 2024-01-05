package ai.freeplay.client.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ChatCompletionResponse {
    private final List<IndexedChatMessage> choices;
    private final AtomicReference<String> completionId = new AtomicReference<>();

    public ChatCompletionResponse(List<IndexedChatMessage> choices) {
        this.choices = choices;
    }

    public Optional<IndexedChatMessage> getFirstChoice() {
        if (choices.isEmpty()) return Optional.empty();
        return Optional.of(choices.get(0));
    }

    public String getContent() {
        if (choices.isEmpty()) return "";
        return choices.get(0).getContent();
    }

    public boolean isComplete() {
        if (choices.isEmpty()) return false;
        return choices.get(0).isComplete();
    }

    public String getCompletionId() {
        return completionId.get();
    }

    public void setCompletionId(String completionId) {
        this.completionId.set(completionId);
    }
}
