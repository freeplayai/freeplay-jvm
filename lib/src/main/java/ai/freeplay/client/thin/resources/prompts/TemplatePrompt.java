package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.TemplateUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.stream.Collectors.toList;

public class TemplatePrompt {
    public static final System.Logger LOGGER = System.getLogger(Freeplay.class.getName());

    private final PromptInfo promptInfo;
    private final List<ChatMessage> messages;

    public TemplatePrompt(PromptInfo promptInfo, List<ChatMessage> messages) {
        this.promptInfo = promptInfo;
        this.messages = messages;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public BoundPrompt bind(Map<String, Object> variables) {
        return bind(variables, null);
    }

    public BoundPrompt bind(Map<String, Object> variables, List<ChatMessage> history) {
        if (!hasHistoryPlaceholder() && history != null) {
            throw new FreeplayClientException(format(
                    "Received history but prompt '%s' does not have a history placeholder.",
                    promptInfo.getTemplateName()
            ));
        }
        if (hasHistoryPlaceholder() && history == null) {
            LOGGER.log(WARNING,
                    "Prompt '{0}' has a history placeholder but no history was provided.",
                    promptInfo.getTemplateName()
            );
        }
        List<ChatMessage> messages = getMessages().stream().flatMap(chatMessage -> {
                    if (chatMessage.isKind()) {
                        if (history != null) {
                            return history.stream();
                        } else {
                            return Stream.of();
                        }
                    } else {
                        return Stream.of(new ChatMessage(
                                chatMessage.getRole(),
                                TemplateUtils.format(chatMessage.getContent(), variables)
                        ));
                    }
                }
        ).collect(toList());
        return new BoundPrompt(promptInfo, messages);
    }

    private boolean hasHistoryPlaceholder() {
        return this.messages.stream()
                .anyMatch(message ->
                        message.isKind() && ((KindMessage) message).getValue().equals("history")
                );
    }
}
