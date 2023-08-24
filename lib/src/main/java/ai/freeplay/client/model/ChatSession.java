package ai.freeplay.client.model;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.internal.CallSupport;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class ChatSession {
    private final CallSupport callSupport;
    private final String sessionId;
    private final PromptTemplate targetTemplate;
    private final List<ChatMessage> messageHistory = new ArrayList<>();

    private Map<String, Object> variables;
    private String tag;
    private String testRunId = null;

    public ChatSession(
            CallSupport callSupport,
            String sessionId,
            Collection<PromptTemplate> prompts,
            String templateName,
            String tag) throws FreeplayException {
        this.callSupport = callSupport;
        this.sessionId = sessionId;
        this.targetTemplate = callSupport.findPrompt(prompts, templateName).orElseThrow(
                () -> new FreeplayException("Cannot find template " + templateName + " in environment " + tag + "."));
    }

    public ChatSession startChat(
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String environment,
            String testRunId,
            ChatFlavor flavor
    ) {
        this.variables = variables;
        this.tag = environment;
        this.testRunId = testRunId;

        ChatFlavor activeFlavor = callSupport.getActiveChatFlavor(flavor, targetTemplate);
        Collection<ChatMessage> formattedMessages = activeFlavor.formatPrompt(targetTemplate.getContent(), this.variables);
        continueChat(formattedMessages, llmParameters);

        return this;
    }

    public ChatCompletionResponse continueChat(ChatMessage newMessage) {
        return continueChat(List.of(newMessage), Collections.emptyMap());
    }

    public ChatCompletionResponse continueChat(ChatMessage newMessage, Map<String, Object> llmParameters) {
        return continueChat(List.of(newMessage), llmParameters);
    }

    public ChatCompletionResponse continueChat(Collection<ChatMessage> newMessages, Map<String, Object> llmParameters) {
        messageHistory.addAll(newMessages);
        List<ChatMessage> cleanMessages = toCleanMessages(messageHistory);
        ChatCompletionResponse response = callSupport.makeContinueChatCall(
                sessionId,
                targetTemplate,
                cleanMessages,
                variables,
                llmParameters,
                tag,
                testRunId);
        if (response.getFirst().isPresent()) {
            messageHistory.add(response.getFirst().get());
        }
        return response;
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
