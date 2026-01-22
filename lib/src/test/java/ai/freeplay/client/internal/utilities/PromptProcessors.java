package ai.freeplay.client.internal.utilities;

import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.processor.ChatPromptProcessor;
import ai.freeplay.client.processor.LLMCallInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PromptProcessors {
    public static final ChatPromptProcessor testChatProcessor = (Collection<ChatMessage> messages, LLMCallInfo info) -> {
        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(1, new ChatMessage("user", "Inserted Message"));
        return newMessages;
    };
}
