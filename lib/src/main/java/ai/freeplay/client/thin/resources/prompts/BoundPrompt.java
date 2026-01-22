package ai.freeplay.client.thin.resources.prompts;

import ai.freeplay.client.thin.LLMAdapters;
import ai.freeplay.client.thin.internal.ThinCallSupport;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO.ToolSchema;

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

        String finalFlavor = ThinCallSupport.getActiveFlavorName(flavorName, promptInfo.getFlavorName());
        LLMAdapters.LLMAdapter<?> llmAdapter = LLMAdapters.adapterForFlavor(finalFlavor);
        //noinspection unchecked
        ContentFormat llmSyntax = (ContentFormat) llmAdapter.toLLMSyntax(messages);

        List<Map<String, Object>> formattedToolSchema = toolSchema != null
            ? llmAdapter.toToolSchemaFormat(toolSchema)
            : null;

        Map<String, Object> formattedOutputSchema = outputSchema != null
            ? llmAdapter.toOutputSchemaFormat(outputSchema)
            : null;

        return new FormattedPrompt<>(getPromptInfo(), getMessages(), llmSyntax, formattedToolSchema, formattedOutputSchema);
    }
}
