package ai.freeplay.client.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ChatCompletionResponse {
    private final List<IndexedChatMessage> choices;

    public ChatCompletionResponse(List<IndexedChatMessage> choices) {
        this.choices = choices;
    }

    public Optional<IndexedChatMessage> getFirst() {
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
}
