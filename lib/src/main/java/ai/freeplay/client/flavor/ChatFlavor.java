package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.IndexedChatMessage;

import java.util.Collection;
import java.util.Map;

public interface ChatFlavor extends Flavor<Collection<ChatMessage>, IndexedChatMessage> {
    ChatCompletionResponse callChatService(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) throws FreeplayException;
}
