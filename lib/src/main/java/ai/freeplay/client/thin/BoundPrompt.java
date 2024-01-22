package ai.freeplay.client.thin;

import java.util.Collection;
import java.util.List;

public class BoundPrompt {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> messages;

    public BoundPrompt(PromptInfo promptInfo, List<ChatMessage> messages) {
        this.promptInfo = promptInfo;
        this.messages = messages;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public Collection<ChatMessage> getMessages() {
        return messages;
    }

    public <Format> Format format(String flavorName) {
        String finalFlavor = ThinCallSupport.getActiveFlavorName(flavorName, promptInfo.getFlavorName());
        LLMAdapters.LLMAdapter<?> llmAdapter = LLMAdapters.adapterForFlavor(finalFlavor);
        //noinspection unchecked
        return (Format) llmAdapter.toLLMSyntax(messages);
    }
}
