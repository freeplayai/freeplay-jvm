package ai.freeplay.client.thin.resources.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Optional<String> getSystemContent() {
        return boundMessages
                .stream()
                .filter(msg -> msg.getRole().equals("system"))
                .findFirst()
                .map(ChatMessage::getContent);
    }

    public List<ChatMessage> allMessages(ChatMessage message) {
        List<ChatMessage> newList = new ArrayList<>(boundMessages);
        newList.add(message);
        return newList;
    }
}
