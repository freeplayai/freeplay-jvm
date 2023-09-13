package ai.freeplay.client.model;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.processor.ChatPromptProcessor;
import ai.freeplay.client.processor.PromptProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CompletionSession {
    private final CallSupport callSupport;
    private final String sessionId;
    private final Collection<PromptTemplate> promptTemplates;
    private final String tag;
    private final String testRunId;

    public CompletionSession(
            CallSupport callSupport,
            String sessionId,
            Collection<PromptTemplate> prompts,
            String environment
    ) {
        this(callSupport, sessionId, prompts, environment, null);
    }

    public CompletionSession(
            CallSupport callSupport,
            String sessionId,
            Collection<PromptTemplate> prompts,
            String environment,
            String testRunId
    ) {
        this.callSupport = callSupport;
        this.sessionId = sessionId;
        this.promptTemplates = prompts;
        this.tag = environment;
        this.testRunId = testRunId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables
    ) {
        return getCompletion(templateName, variables, Collections.emptyMap(), null, null);
    }

    public CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters
    ) {
        return getCompletion(templateName, variables, llmParameters, null, null);
    }

    public <P, R> CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            PromptProcessor<P> promptProcessor
    ) {
        return getCompletion(templateName, variables, llmParameters, null, promptProcessor);
    }

    public <P, R> CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            Flavor<P, R> flavor
    ) {
        return getCompletion(templateName, variables, llmParameters, flavor, null);
    }

    public <P, R> CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            Flavor<P, R> flavor,
            PromptProcessor<P> promptProcessor
    ) {
        return callSupport.prepareAndMakeCall(
                getSessionId(), promptTemplates,
                templateName,
                variables,
                llmParameters,
                tag,
                testRunId,
                flavor,
                promptProcessor
        );
    }

    public <P, R> Stream<R> getCompletionStream(
            String templateName,
            Map<String, Object> variables
    ) {
        return getCompletionStream(templateName, variables, Collections.emptyMap(), null, null, null);
    }

    public <P, R> Stream<R> getCompletionStream(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters
    ) {
        return getCompletionStream(templateName, variables, llmParameters, null, null, null);
    }

    public <P, R> Stream<R> getCompletionStream(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P, R> flavor
    ) {
        return getCompletionStream(
                templateName,
                variables,
                llmParameters,
                testRunId,
                flavor,
                null
        );
    }

    public <P, R> Stream<R> getCompletionStream(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            PromptProcessor<P> promptProcessor
    ) {
        return getCompletionStream(
                templateName,
                variables,
                llmParameters,
                testRunId,
                null,
                promptProcessor
        );
    }

    public <P, R> Stream<R> getCompletionStream(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P, R> flavor,
            PromptProcessor<P> promptProcessor
    ) {
        PromptTemplate template = callSupport
                .findPrompt(promptTemplates, templateName)
                .orElseThrow(() -> new FreeplayException(
                        "Unable to find prompt template with name " + templateName + " in environment " + tag));

        return callSupport.makeCallStream(
                getSessionId(),
                template,
                variables,
                llmParameters,
                tag,
                testRunId,
                flavor,
                promptProcessor
        );
    }

    @SuppressWarnings("unused")
    public ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables
    ) {
        return getChatCompletion(templateName, variables, Collections.emptyMap(), null, null, null);
    }

    @SuppressWarnings("unused")
    public ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters
    ) {
        return getChatCompletion(templateName, variables, llmParameters, null, null, null);
    }

    public ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            ChatPromptProcessor promptProcessor
    ) {
        return getChatCompletion(templateName, variables, llmParameters, null, null, promptProcessor);
    }

    public ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<ChatMessage, IndexedChatMessage> flavor,
            ChatPromptProcessor promptProcessor
    ) {
        return callSupport.makeContinueChatCall(
                sessionId,
                promptTemplates,
                templateName,
                variables,
                llmParameters,
                tag,
                testRunId,
                flavor,
                promptProcessor);
    }
}
