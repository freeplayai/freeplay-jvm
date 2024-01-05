package ai.freeplay.client.model;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.LLMServerException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.internal.CallSupport;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ChatSession {
    private final CallSupport callSupport;
    private final String sessionId;
    private final PromptTemplate targetTemplate;
    private final List<ChatMessage> messageHistory = new ArrayList<>();

    private Map<String, Object> variables;
    private String tag;
    private final String testRunId = null;
    private final Map<String, Object> customMetadata;

    public ChatSession(
            CallSupport callSupport,
            String sessionId,
            Collection<PromptTemplate> prompts,
            String templateName,
            String tag,
            Map<String, Object> customMetadata
    ) throws FreeplayException {
        this.callSupport = callSupport;
        this.sessionId = sessionId;
        this.targetTemplate = callSupport.findPrompt(prompts, templateName).orElseThrow(
                () -> new FreeplayConfigurationException("Cannot find template " + templateName + " in environment " + tag + "."));
        this.customMetadata = customMetadata;
    }

    public ChatStart<IndexedChatMessage> startChat(
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) {
        this.variables = variables;
        this.tag = environment;

        ChatFlavor activeFlavor = callSupport.getActiveFlavor(flavor, targetTemplate);
        Collection<ChatMessage> formattedMessages = activeFlavor.formatPrompt(targetTemplate.getContent(), this.variables);
        ChatCompletionResponse response = continueChat(formattedMessages, llmParameters);

        return new ChatStart<>(
                this,
                response
                        .getFirstChoice()
                        .orElseThrow(() -> new LLMServerException("Did not receive a choice within the chat response.")));
    }

    @SuppressWarnings("UnusedReturnValue")
    public ChatCompletionResponse continueChat(
            ChatMessage newMessage
    ) {
        return continueChat(List.of(newMessage), Collections.emptyMap());
    }

    public ChatCompletionResponse continueChat(
            ChatMessage newMessage,
            Map<String, Object> llmParameters
    ) {
        return continueChat(List.of(newMessage), llmParameters);
    }

    public ChatCompletionResponse continueChat(
            Collection<ChatMessage> newMessages,
            Map<String, Object> llmParameters
    ) {
        messageHistory.addAll(newMessages);
        List<ChatMessage> cleanMessages = toCleanMessages(messageHistory);
        ChatCompletionResponse response = callSupport.makeContinueChatCall(
                sessionId,
                targetTemplate,
                cleanMessages,
                variables,
                llmParameters,
                this.customMetadata,
                tag,
                testRunId,
                null);
        if (response.getFirstChoice().isPresent()) {
            messageHistory.add(response.getFirstChoice().get());
        }
        return response;
    }

    public Stream<IndexedChatMessage> startChatStream(
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            ChatFlavor flavor
    ) {
        this.variables = variables;
        this.tag = environment;

        ChatFlavor activeFlavor = callSupport.getActiveFlavor(flavor, targetTemplate);
        Collection<ChatMessage> formattedMessages =
                activeFlavor.formatPrompt(targetTemplate.getContent(), this.variables);
        return continueChatStream(formattedMessages, llmParameters);
    }

    public Stream<IndexedChatMessage> continueChatStream(
            ChatMessage newMessage,
            Map<String, Object> llmParameters
    ) {
        return continueChatStream(List.of(newMessage), llmParameters);
    }

    public Stream<IndexedChatMessage> continueChatStream(
            Collection<ChatMessage> newMessages,
            Map<String, Object> llmParameters
    ) {
        messageHistory.addAll(newMessages);
        List<ChatMessage> cleanMessages = toCleanMessages(messageHistory);
        Stream<IndexedChatMessage> response = callSupport.makeContinueChatCallStream(
                sessionId,
                targetTemplate,
                cleanMessages,
                variables,
                llmParameters,
                this.customMetadata,
                tag,
                testRunId);

        AtomicReference<String> aggregatedContent = new AtomicReference<>("");
        return response.peek((IndexedChatMessage message) -> {
            aggregatedContent.getAndUpdate((String previous) -> previous + message.getContent());
            if (message.isLast()) {
                messageHistory.add(new ChatMessage(message.getRole(), aggregatedContent.get()));
            }
        });
    }

    public List<ChatMessage> getMessageHistory() {
        return messageHistory;
    }

    public Optional<ChatMessage> getLastMessage() {
        if (messageHistory.isEmpty()) return Optional.empty();
        return Optional.of(messageHistory.get(messageHistory.size() - 1));
    }

    private List<ChatMessage> toCleanMessages(List<ChatMessage> messageHistory) {
        return messageHistory.stream().map((ChatMessage message) ->
                message instanceof IndexedChatMessage ?
                        new ChatMessage(message.getRole(), message.getContent()) :
                        message
        ).collect(toList());
    }
}
