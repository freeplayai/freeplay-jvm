package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.thin.LLMAdapters;
import ai.freeplay.client.thin.internal.ThinCallSupport;

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

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public <Format> FormattedPrompt<Format> format() {
        return format(null);
    }

    public <Format> FormattedPrompt<Format> format(String flavorName) {
        String finalFlavor = ThinCallSupport.getActiveFlavorName(flavorName, promptInfo.getFlavorName());
        LLMAdapters.LLMAdapter<?> llmAdapter = LLMAdapters.adapterForFlavor(finalFlavor);
        //noinspection unchecked
        Format llmSyntax = (Format) llmAdapter.toLLMSyntax(messages);
        return new FormattedPrompt<>(getPromptInfo(), getMessages(), llmSyntax);
    }
}
