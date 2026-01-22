package ai.freeplay.client.thin.resources.prompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormattedPrompt<LLMContentFormat> {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> boundMessages;
    private final LLMContentFormat formattedPrompt;
    private final List<Map<String, Object>> toolSchema;
    private Map<String, Object> outputSchema;

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMContentFormat formattedPrompt) {
        this(promptInfo, messages, formattedPrompt, null, null);
    }

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMContentFormat formattedPrompt, List<Map<String, Object>> toolSchema) {
        this(promptInfo, messages, formattedPrompt, toolSchema, null);
    }

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMContentFormat formattedPrompt, List<Map<String, Object>> toolSchema, Map<String, Object> outputSchema) {
        this.promptInfo = promptInfo;
        this.boundMessages = messages;
        this.formattedPrompt = formattedPrompt;
        this.toolSchema = toolSchema;
        this.outputSchema = outputSchema;
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
        try {
            return boundMessages
                    .stream()
                    .filter(msg -> msg.getRole().equals("system"))
                    .findFirst()
                    .map(ChatMessage::getContent);
        } catch (IllegalStateException e) {
            // System message must be a string
            throw new IllegalStateException("System message must be a string");
        }
    }

    public List<Map<String, Object>> getToolSchema() {
        return toolSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public FormattedPrompt<LLMContentFormat> outputSchema(Map<String, Object> outputSchema) {
        this.outputSchema = outputSchema;
        return this;
    }

    public List<ChatMessage> allMessages(ChatMessage message) {
        List<ChatMessage> newList = new ArrayList<>(boundMessages);
        newList.add(message);
        return newList;
    }

    public List<ChatMessage> allMessages(Object completionMessage) {
        List<ChatMessage> newList = new ArrayList<>(boundMessages);
        newList.add(new ChatMessage(completionMessage));
        return newList;
    }
}
