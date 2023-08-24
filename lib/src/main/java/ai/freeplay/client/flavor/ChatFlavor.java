package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.ChatMessage;

import java.util.Collection;
import java.util.Map;

public interface ChatFlavor extends Flavor<Collection<ChatMessage>> {
    ChatCompletionResponse callChatService(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters
    ) throws FreeplayException;
}
