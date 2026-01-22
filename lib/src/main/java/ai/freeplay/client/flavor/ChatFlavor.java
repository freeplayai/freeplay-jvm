package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public interface ChatFlavor {
    ChatCompletionResponse callChatService(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) throws FreeplayException;

    String getFormatType();

    default String getProvider() {
        return getProviderEnum().getName();
    }

    Provider getProviderEnum();

    Collection<ChatMessage> formatPrompt(String template, Map<String, Object> variables);

    CompletionResponse callService(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException;

    Stream<IndexedChatMessage> callServiceStream(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException;

    String serializeForRecord(Collection<ChatMessage> formattedPrompt);

    String getContentFromChunk(IndexedChatMessage chunk);

    boolean isLastChunk(IndexedChatMessage chunk);

    boolean isComplete(IndexedChatMessage chunk);

    ChatFlavor DEFAULT = null;
}
