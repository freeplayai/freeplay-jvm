package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.CompletionResponse;

import java.util.Map;

public interface Flavor<P> {
    P formatPrompt(String template, Map<String, Object> variables);

    CompletionResponse callService(String formattedPrompt, ProviderConfig providerConfig, Map<String, Object> llmParameters) throws FreeplayException;

    String getFormatType();

    String getProvider();
}
