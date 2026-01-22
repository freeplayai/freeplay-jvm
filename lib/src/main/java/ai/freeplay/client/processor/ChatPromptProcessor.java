package ai.freeplay.client.processor;

import ai.freeplay.client.model.ChatMessage;

import java.util.Collection;
import java.util.function.BiFunction;

public interface ChatPromptProcessor extends BiFunction<Collection<ChatMessage>, LLMCallInfo, Collection<ChatMessage>> {
    ChatPromptProcessor DEFAULT = null;
}
