package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.media.MediaInputUrl;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO.ToolSchema;

import java.util.ArrayList;
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
    private List<ToolSchema> toolSchema;
    private Map<String, Object> outputSchema;

    public TemplatePrompt(PromptInfo promptInfo, List<ChatMessage> messages) {
        this(promptInfo, messages, null);
    }

    public TemplatePrompt(PromptInfo promptInfo, List<ChatMessage> messages, List<ToolSchema> toolSchema) {
        this.promptInfo = promptInfo;
        this.messages = messages;
        this.toolSchema = toolSchema;
        this.outputSchema = null;
    }

    public TemplatePrompt outputSchema(Map<String, Object> outputSchema) {
        this.outputSchema = outputSchema;
        return this;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<ToolSchema> getToolSchema() {
        return toolSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    /**
     * @deprecated use {@link #bind(TemplatePrompt.BindRequest)} instead.
     */
    @Deprecated
    public BoundPrompt bind(Map<String, Object> variables) {
        return bind(new BindRequest(variables));
    }

    /**
     * @deprecated use {@link #bind(TemplatePrompt.BindRequest)} instead.
     */
    @Deprecated
    public BoundPrompt bind(Map<String, Object> variables, List<ChatMessage> history) {
        return bind(new BindRequest(variables).history(history));
    }

    public BoundPrompt bind(BindRequest bindRequest) {
        var history = bindRequest.getHistory();
        if (getMessages().stream().anyMatch(message -> message.isStructuredMessage() || message.isCompletionMessage())) {
            throw new FreeplayClientException("StructuredMessage or CompletionMessage is not allowed when binding a prompt");
        }

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
                        var contentParts = extractContentParts(chatMessage, bindRequest.getMediaInputs());
                        String substitutedContent = TemplateUtils.format(chatMessage.getContent(), bindRequest.getVariables());

                        if (contentParts.isEmpty()) {
                            return Stream.of(new ChatMessage(chatMessage.getRole(), substitutedContent));
                        } else {
                            List<Object> messageContent = new ArrayList<>(contentParts.size() + 1);
                            messageContent.add(new ContentPartText(substitutedContent));
                            messageContent.addAll(contentParts);

                            return Stream.of(new ChatMessage(
                                    chatMessage.getRole(),
                                    messageContent
                            ));
                        }
                    }
                }
        ).collect(toList());

        BoundPrompt boundPrompt = new BoundPrompt(promptInfo, messages, toolSchema);
        if (outputSchema != null) {
            boundPrompt.outputSchema(outputSchema);
        }
        return boundPrompt;
    }

    public static class BindRequest {
        private Map<String, Object> variables;
        private List<ChatMessage> history;
        private MediaInputCollection mediaInputs;

        public BindRequest(Map<String, Object> variables) {
            this.variables = variables;
        }

        public BindRequest history(List<ChatMessage> history) {
            this.history = history;
            return this;
        }

        public BindRequest mediaInputs(MediaInputCollection mediaInputs) {
            this.mediaInputs = mediaInputs;
            return this;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public List<ChatMessage> getHistory() {
            return history;
        }

        public MediaInputCollection getMediaInputs() {
            return mediaInputs;
        }

        @Override
        public String toString() {
            return "BindRequest{" +
                    "variables=" + variables +
                    ", history=" + history +
                    ", mediaInputs=" + mediaInputs +
                    '}';
        }
    }

    private List<ContentPart> extractContentParts(ChatMessage message, MediaInputCollection mediaInputs) {
        List<ContentPart> result = new ArrayList<>();
        for (MediaSlot slot : message.getMediaSlots()) {
            var maybeMatch = mediaInputs.get(slot.getPlaceholderName());
            if (maybeMatch.isEmpty()) {
                continue;
            }
            var match = maybeMatch.get();
            if (match instanceof MediaInputUrl) {
                MediaInputUrl url = (MediaInputUrl) match;
                result.add(new ContentPartUrl(slot.getPlaceholderName(), slot.getType(), url.getUrl()));
            } else if (match instanceof MediaInputBase64) {
                MediaInputBase64 base64 = (MediaInputBase64) match;
                result.add(new ContentPartBase64(slot.getPlaceholderName(), slot.getType(), base64.getContentType(), base64.getData()));
            }
        }

        return result;
    }

    private boolean hasHistoryPlaceholder() {
        return this.messages.stream()
                .anyMatch(message ->
                        message.isKind() && ((KindMessage) message).getValue().equals("history")
                );
    }
}
