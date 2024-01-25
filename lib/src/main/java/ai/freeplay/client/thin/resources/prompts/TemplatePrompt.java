package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.internal.TemplateUtils;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class TemplatePrompt {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> messages;

    public TemplatePrompt(PromptInfo promptInfo, List<ChatMessage> messages) {
        this.promptInfo = promptInfo;
        this.messages = messages;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public BoundPrompt bind(Map<String, Object> variables) {
        List<ChatMessage> messages = getMessages().stream().map(chatMessage ->
                new ChatMessage(
                        chatMessage.getRole(),
                        TemplateUtils.format(chatMessage.getContent(), variables)
                )).collect(toList());
        return new BoundPrompt(promptInfo, messages);
    }
}
