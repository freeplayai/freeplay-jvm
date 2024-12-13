package ai.freeplay.client.thin.resources.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormattedPrompt<LLMContentFormat> {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> boundMessages;
    private final LLMContentFormat formattedPrompt;
    private final Map<String, Object> toolSchema;

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMContentFormat formattedPrompt) {
        this(promptInfo, messages, formattedPrompt, null);
    }

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMContentFormat formattedPrompt, Map<String, Object> toolSchema) {
        this.promptInfo = promptInfo;
        this.boundMessages = messages;
        this.formattedPrompt = formattedPrompt;
        this.toolSchema = toolSchema;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public List<ChatMessage> getBoundMessages() {
        return boundMessages;
    }

    public LLMContentFormat getFormattedPrompt() {
        return formattedPrompt;
    }

    public Optional<String> getSystemContent() {
        return boundMessages
                .stream()
                .filter(msg -> msg.getRole().equals("system"))
                .findFirst()
                .map(ChatMessage::getContent);
    }

    public Map<String, Object> getToolSchema() {
        return toolSchema;
    }

    public List<ChatMessage> allMessages(ChatMessage message) {
        List<ChatMessage> newList = new ArrayList<>(boundMessages);
        newList.add(message);
        return newList;
    }
}
