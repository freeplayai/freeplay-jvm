package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.Provider;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @param <P> The type of the prompt (i.e. the 'input')
 * @param <R> The type of the response (i.e. the 'output')
 */
public interface Flavor<P, R> {
    String getFormatType();

    default String getProvider() {
        return getProviderEnum().getName();
    }

    Provider getProviderEnum();

    P formatPrompt(String template, Map<String, Object> variables);

    CompletionResponse callService(
            P formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException;

    Stream<R> callServiceStream(
            P formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException;

    String serializeForRecord(P formattedPrompt);

    String getContentFromChunk(R chunk);

    boolean isLastChunk(R chunk);

    boolean isComplete(R chunk);

    @SuppressWarnings("rawtypes")
    Flavor DEFAULT = null;
}
