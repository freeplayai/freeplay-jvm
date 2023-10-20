package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.model.*;
import ai.freeplay.client.processor.PromptProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Freeplay {
    private final CallSupport callSupport;

    public Freeplay(String freeplayAPIKey, String baseUrl, ProviderConfigs providerConfigs) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, new HttpConfig());
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            Map<String, Object> llmParameters
    ) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, llmParameters, new HttpConfig());
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey, baseUrl, providerConfigs, null, llmParameters, httpConfig);
    }

    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfigs providerConfigs,
            Flavor<?, ?> flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        callSupport = new CallSupport(
                freeplayAPIKey,
                baseUrl,
                providerConfigs,
                flavor,
                llmParameters,
                httpConfig);
    }

    // ====================================================
    // Backward compatible constructors
    // ====================================================

    @Deprecated
    public Freeplay(String freeplayAPIKey, String baseUrl, ProviderConfig providerConfig) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, null, new HttpConfig());
    }

    @Deprecated
    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters
    ) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, llmParameters, new HttpConfig());
    }

    @Deprecated
    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey, baseUrl, providerConfig, null, llmParameters, httpConfig);
    }

    @Deprecated
    public Freeplay(
            String freeplayAPIKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Flavor<?, ?> flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) {
        this(freeplayAPIKey,
                baseUrl,
                ProviderConfigs.fromGenericConfig(providerConfig),
                flavor,
                llmParameters,
                httpConfig
        );
    }

    public CompletionSession createSession(String projectId, String environment) {
        String sessionId = callSupport.createSession(projectId, environment);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);

        return new CompletionSession(callSupport, sessionId, prompts, environment);
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            String environment
    ) throws FreeplayException {
        return getCompletion(projectId, templateName, variables, Collections.emptyMap(), environment);
    }

    public CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        return getCompletion(projectId, templateName, variables, llmParameters, environment, null, null);
    }

    public <P> CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            Flavor<P, CompletionResponse> flavor
    ) throws FreeplayException {
        return getCompletion(
                projectId,
                templateName,
                variables,
                llmParameters,
                environment,
                flavor,
                null
        );
    }

    public <P> CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            PromptProcessor<P> promptProcessor
    ) throws FreeplayException {
        return getCompletion(
                projectId,
                templateName,
                variables,
                llmParameters,
                environment,
                null,
                promptProcessor
        );
    }

    public <P> CompletionResponse getCompletion(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            Flavor<P, CompletionResponse> flavor,
            PromptProcessor<P> promptProcessor
    ) throws FreeplayException {
        String sessionId = callSupport.createSession(projectId, environment);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        return callSupport.prepareAndMakeCall(
                sessionId,
                prompts,
                templateName,
                variables,
                llmParameters,
                environment,
                null,   // testRunId
                flavor,
                promptProcessor
        );
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            String environment
    ) throws FreeplayException {
        return startChat(projectId, templateName, variables, Collections.emptyMap(), environment);
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        return startChat(projectId, templateName, variables, llmParameters, environment, null);
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) throws FreeplayException {
        String sessionId = callSupport.createSession(projectId, environment);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        ChatSession chatSession =
                new ChatSession(
                        callSupport,
                        sessionId,
                        prompts,
                        templateName,
                        environment);
        return chatSession.startChat(
                variables,
                llmParameters,
                environment,
                flavor
        );
    }

    public ChatStart<Stream<IndexedChatMessage>> startChatStream(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        return startChatStream(projectId, templateName, variables, llmParameters, environment, null);
    }

    public ChatStart<Stream<IndexedChatMessage>> startChatStream(
            String projectId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) throws FreeplayException {
        String sessionId = callSupport.createSession(projectId, environment);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        ChatSession chatSession = new ChatSession(callSupport, sessionId, prompts, templateName, environment);
        return new ChatStart<>(
                chatSession,
                chatSession.startChatStream(
                        variables,
                        llmParameters,
                        environment,
                        flavor
                ));
    }

    public TestRun createTestRun(String projectId, String environment, String testListName) {
        return callSupport.createTestRun(projectId, environment, testListName);
    }
}
