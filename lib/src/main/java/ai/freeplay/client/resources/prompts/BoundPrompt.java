package ai.freeplay.client.resources.prompts;

import ai.freeplay.client.adapters.LLMAdapters;
import ai.freeplay.client.adapters.RoleSupport;
import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.internal.v2dto.TemplateDTO.ToolSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoundPrompt {
    private final PromptInfo promptInfo;
    private final List<ChatMessage> messages;
    private List<ToolSchema> toolSchema;
    private Map<String, Object> outputSchema;

    public BoundPrompt(PromptInfo promptInfo, List<ChatMessage> messages) {
        this(promptInfo, messages, null);
    }

    public BoundPrompt(PromptInfo promptInfo, List<ChatMessage> messages, List<ToolSchema> toolSchema) {
        this.promptInfo = promptInfo;
        this.messages = messages;
        this.toolSchema = toolSchema;
        this.outputSchema = null;
    }

    public BoundPrompt outputSchema(Map<String, Object> outputSchema) {
        this.outputSchema = outputSchema;
        return this;
    }

    public PromptInfo getPromptInfo() {
        return promptInfo;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public <ContentFormat> FormattedPrompt<ContentFormat> format() {
        return format(null);
    }

    public <ContentFormat> FormattedPrompt<ContentFormat> format(String flavorName) {

        String finalFlavor = CallSupport.getActiveFlavorName(flavorName, promptInfo.getFlavorName());
        LLMAdapters.LLMAdapter<?> llmAdapter = LLMAdapters.adapterForFlavor(finalFlavor);

        PromptInfo effectivePromptInfo = flavorName != null
            ? new PromptInfo(
                promptInfo.getPromptTemplateId(),
                promptInfo.getPromptTemplateVersionId(),
                promptInfo.getTemplateName(),
                promptInfo.getEnvironment(),
                promptInfo.getModelParameters(),
                llmAdapter.getProvider(),
                promptInfo.getModel(),
                finalFlavor
            )
            : promptInfo;

        List<ChatMessage> prepared = RoleSupport.prepareMessages(messages, llmAdapter.getRoleSupport(), finalFlavor);

        //noinspection unchecked
        ContentFormat llmSyntax = (ContentFormat) llmAdapter.toLLMSyntax(prepared);

        // Kind of a hack: bound messages contain internal content objects
        // (ContentPartText, ContentPartBase64, etc.) that aren't provider-formatted.
        // Ideally bind() wouldn't produce these internal objects.
        // For now, re-run each structured message through the adapter to get
        // provider-formatted content while preserving messages the adapter
        // would otherwise drop (e.g. system for Responses API).
        List<ChatMessage> convertedMessages = new ArrayList<>(prepared.size());
        for (ChatMessage msg : prepared) {
            if (msg.isStructuredMessage()) {
                Object converted = llmAdapter.toLLMSyntax(List.of(msg));
                if (converted instanceof List
                        && !((List<?>) converted).isEmpty()
                        && ((List<?>) converted).get(0) instanceof ChatMessage) {
                    convertedMessages.add((ChatMessage) ((List<?>) converted).get(0));
                } else {
                    convertedMessages.add(msg);
                }
            } else {
                convertedMessages.add(msg);
            }
        }
        prepared = convertedMessages;

        List<Map<String, Object>> formattedToolSchema = toolSchema != null
            ? llmAdapter.toToolSchemaFormat(toolSchema)
            : null;

        Map<String, Object> formattedOutputSchema = outputSchema != null
            ? llmAdapter.toOutputSchemaFormat(outputSchema)
            : null;

        return new FormattedPrompt<>(effectivePromptInfo, prepared, llmSyntax, formattedToolSchema, formattedOutputSchema);
    }
}
