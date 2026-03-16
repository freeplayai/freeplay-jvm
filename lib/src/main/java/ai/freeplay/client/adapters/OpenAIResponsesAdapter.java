package ai.freeplay.client.adapters;

import ai.freeplay.client.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.resources.prompts.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class OpenAIResponsesAdapter implements LLMAdapters.LLMAdapter<List<Map<String, Object>>> {

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
        return messages.stream()
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
            List<Object> converted = message.getStructuredContent().stream()
                    .map(this::toResponsesContentPart)
                    .collect(toList());
            map.put("content", converted);
        } else if (message.isCompletionMessage()) {
            map.put("content", message.getCompletionMessage());
        }
        return map;
    }

    private Object toResponsesContentPart(Object part) {
        if (part instanceof TextContent) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_text");
            m.put("text", ((TextContent) part).getText());
            return m;
        } else if (part instanceof ImageUrlContent) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_image");
            m.put("image_url", ((ImageUrlContent) part).getUrl());
            return m;
        } else if (part instanceof ImageContent) {
            ImageContent img = (ImageContent) part;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_image");
            m.put("image_url", String.format("data:%s;base64,%s", img.getContentType(), img.getData()));
            return m;
        } else if (part instanceof FileContent) {
            FileContent file = (FileContent) part;
            String contentFormat = file.getContentType().split("/")[1];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_file");
            m.put("filename", String.format("%s.%s", file.getFilename(), contentFormat));
            m.put("file_data", String.format("data:%s;base64,%s", file.getContentType(), file.getData()));
            return m;
        } else if (part instanceof AudioContent) {
            throw new IllegalStateException("Audio content is not yet supported by the OpenAI Responses API.");
        }
        return part;
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
