package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.PromptTemplate;
import ai.freeplay.client.model.Session;

import java.util.Collection;
import java.util.Map;

public class Freeplay {
    private final CallSupport callSupport;

    public Freeplay(String freeplayAPIKey, String baseUrl, ProviderConfig providerConfig) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, null);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters
    ) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, llmParameters);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Flavor<?> flavor,
            Map<String, Object> llmParameters
    ) {
        callSupport = new CallSupport(
                freeplayAPIKey,
                baseUrl,
                providerConfig,
                flavor,
                llmParameters);
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        Session session = callSupport.createSession(projectId, environment);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        return callSupport.prepareAndMakeCall(
                prompts,
                session.getSessionId(),
                templateName,
                variables,
                llmParameters,
                environment,
                null,   // testRunId
                null    // flavor
        );
    }
}
