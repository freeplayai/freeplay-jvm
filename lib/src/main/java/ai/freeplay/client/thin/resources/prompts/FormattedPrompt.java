package ai.freeplay.client.thin.resources.prompts;

import java.util.ArrayList;
import java.util.List;

public class FormattedPrompt<LLMFormat> {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> boundMessages;
    private final LLMFormat formattedPrompt;

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMFormat formattedPrompt) {
        this.promptInfo = promptInfo;
        this.boundMessages = messages;
        this.formattedPrompt = formattedPrompt;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public List<ChatMessage> getBoundMessages() {
        return boundMessages;
    }

    public LLMFormat getFormattedPrompt() {
        return formattedPrompt;
    }

    public List<ChatMessage> allMessages(ChatMessage message) {
        List<ChatMessage> newList = new ArrayList<>(boundMessages);
        newList.add(message);
        return newList;
    }
}
