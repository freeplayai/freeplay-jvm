package ai.freeplay.client.model;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.CallSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class CompletionSession {
    private final CallSupport callSupport;
    private final String sessionId;
    private final Collection<PromptTemplate> prompts;
    private final String tag;

    public CompletionSession(
            CallSupport callSupport,
            String sessionId,
            Collection<PromptTemplate> prompts,
            String environment
    ) {
        this.callSupport = callSupport;
        this.sessionId = sessionId;
        this.prompts = prompts;
        this.tag = environment;
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

    public <P> CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P> flavor
    ) {
        return callSupport.prepareAndMakeCall(
                getSessionId(), prompts,
                templateName,
                variables,
                llmParameters,
                tag,
                testRunId,
                flavor
        );
    }

    @SuppressWarnings("unused")
    public ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables
    ) {
        return getChatCompletion(templateName, variables, Collections.emptyMap(), null, null);
    }

    @SuppressWarnings("unused")
    public ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters
    ) {
        return getChatCompletion(templateName, variables, llmParameters, null, null);
    }

    @SuppressWarnings("unused")
    public <P> ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P> flavor
    ) {
        Optional<PromptTemplate> maybePrompt = callSupport.findPrompt(prompts, templateName);
        return maybePrompt.map((PromptTemplate prompt) -> {
            ChatFlavor activeFlavor = callSupport.getActiveChatFlavor(flavor, prompt);
            Collection<ChatMessage> formattedPrompt = activeFlavor.formatPrompt(prompt.getContent(), variables);
            return callSupport.makeContinueChatCall(
                    getSessionId(),
                    prompt,
                    formattedPrompt,
                    variables,
                    llmParameters,
                    tag,
                    testRunId
            );
        }).orElseThrow(() ->
                new FreeplayException(format("Prompt template %s not found in environment %s.", templateName, tag))
        );
    }
}
