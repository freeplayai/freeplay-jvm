package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.CompletionResponse;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @param <P> The type of the prompt (i.e. the 'input')
 * @param <R> The type of the response (i.e. the 'output')
 */
public interface Flavor<P, R> {
    String getFormatType();

    String getProvider();

    P formatPrompt(String template, Map<String, Object> variables);

    CompletionResponse callService(P formattedPrompt, ProviderConfig providerConfig, Map<String, Object> llmParameters) throws FreeplayException;

    Stream<R> callServiceStream(
            P formattedPrompt,
            ProviderConfig providerConfig,
            Map<String, Object> mergedLLMParameters
    );

    String serializeForRecord(P formattedPrompt);

    String getContentFromChunk(R chunk);

    boolean isLastChunk(R chunk);

    boolean isComplete(R chunk);
}
