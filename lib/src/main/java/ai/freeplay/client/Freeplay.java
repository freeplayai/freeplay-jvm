package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.model.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

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
            Flavor<?, ?> flavor,
            Map<String, Object> llmParameters
    ) {
        callSupport = new CallSupport(
                freeplayAPIKey,
                baseUrl,
                providerConfig,
                flavor,
                llmParameters);
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
                null    // flavor
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
        return startChat(projectId, null, templateName, variables, llmParameters, environment);
    }

    public ChatStart<IndexedChatMessage> startChat(
            String projectId,
            ChatFlavor flavor,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
    ) throws FreeplayException {
        String sessionId = callSupport.createSession(projectId, environment);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        ChatSession chatSession = new ChatSession(callSupport, sessionId, prompts, templateName, environment);
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
        return startChatStream(projectId, null, templateName, variables, llmParameters, environment);
    }

    public ChatStart<Stream<IndexedChatMessage>> startChatStream(
            String projectId,
            ChatFlavor flavor,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment
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
