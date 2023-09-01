package ai.freeplay.client.model;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.CallSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

public class CompletionSession {
    private final CallSupport callSupport;
    private final String sessionId;
    private final Collection<PromptTemplate> promptTemplates;
    private final String tag;

    public CompletionSession(
            CallSupport callSupport,
            String sessionId,
            Collection<PromptTemplate> prompts,
            String environment
    ) {
        this.callSupport = callSupport;
        this.sessionId = sessionId;
        this.promptTemplates = prompts;
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

    public <P, R> CompletionResponse getCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P, R> flavor
    ) {
        return callSupport.prepareAndMakeCall(
                getSessionId(), promptTemplates,
                templateName,
                variables,
                llmParameters,
                tag,
                testRunId,
                flavor
        );
    }

    public <P, R> Stream<R> getCompletionStream(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P, R> flavor
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
    public <P, R> ChatCompletionResponse getChatCompletion(
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String testRunId,
            Flavor<P, R> flavor
    ) {
        Optional<PromptTemplate> maybePrompt = callSupport.findPrompt(promptTemplates, templateName);
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
