package ai.freeplay.client.thin;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class FormattedPrompt<LLMFormat> {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> boundMessages;
    private final LLMFormat formattedPrompt;

    public FormattedPrompt(PromptInfo promptInfo, List<ChatMessage> messages, LLMFormat formattedPrompt) {
        this.promptInfo = promptInfo;
        this.boundMessages = messages;
        if (isListOfThickMessages(formattedPrompt)) {
            //noinspection unchecked
            this.formattedPrompt = (LLMFormat) toThinChatMessages((List<ai.freeplay.client.model.ChatMessage>) formattedPrompt);
        } else {
            this.formattedPrompt = formattedPrompt;
        }
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

    private static <LLMFormat> boolean isListOfThickMessages(LLMFormat formattedPrompt) {
        // This is ugly, but because we don't want to expose the Thick client's ChatMessage class in the thin.
        // We are looking for a clean way to remove the need for this.
        return formattedPrompt instanceof List &&
                !((List<?>) formattedPrompt).isEmpty() &&
                ((List<?>) formattedPrompt).get(0) instanceof ai.freeplay.client.model.ChatMessage;
    }

    private static List<ChatMessage> toThinChatMessages(List<ai.freeplay.client.model.ChatMessage> modelMessages) {
        return modelMessages.stream()
                .map(message -> new ChatMessage(message.getRole(), message.getContent()))
                .collect(toList());
    }
}
