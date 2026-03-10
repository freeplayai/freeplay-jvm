package ai.freeplay.client.adapters;

import ai.freeplay.client.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.resources.prompts.ChatMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class OpenAIResponsesAdapter implements LLMAdapters.LLMAdapter<List<Map<String, Object>>> {
    private final OpenAILLMAdapter delegate = new OpenAILLMAdapter();

    @Override
    public RoleSupport getRoleSupport() {
        return RoleSupport.OPENAI_RESPONSES;
    }

    @Override
    public String getProvider() {
        return "openai";
    }

    @Override
    public List<Map<String, Object>> toLLMSyntax(List<ChatMessage> messages) {
        List<ChatMessage> formatted = delegate.toLLMSyntax(messages);
        return formatted.stream()
                .filter(m -> !"system".equals(m.getRole()))
                .map(this::toResponsesMessage)
                .collect(toList());
    }

    private Map<String, Object> toResponsesMessage(ChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "message");
        map.put("role", message.getRole());
        if (message.isStringMessage()) {
            map.put("content", message.getContent());
        } else if (message.isStructuredMessage()) {
            map.put("content", message.getStructuredContent());
        } else if (message.isCompletionMessage()) {
            map.put("content", message.getCompletionMessage());
        }
        return map;
    }

    @Override
    public List<Map<String, Object>> toToolSchemaFormat(List<ToolSchema> toolSchema) {
        if (toolSchema == null) {
            return null;
        }
        return toolSchema.stream()
                .map(schema -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("type", "function");
                    map.put("name", schema.getName());
                    map.put("description", schema.getDescription());
                    map.put("parameters", schema.getParameters());
                    return map;
                })
                .collect(toList());
    }

    @Override
    public Map<String, Object> toOutputSchemaFormat(Map<String, Object> outputSchema) {
        return outputSchema;
    }
}
