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
        if (part instanceof ContentPartText) {
            ContentPartText text = (ContentPartText) part;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_text");
            m.put("text", text.getText());
            return m;
        } else if (part instanceof ContentPartUrl) {
            ContentPartUrl url = (ContentPartUrl) part;
            if (url.getType() != MediaType.IMAGE) {
                throw new IllegalStateException("Message contains a non-image URL, but OpenAI Responses API only supports image URLs.");
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_image");
            m.put("image_url", url.getUrl());
            return m;
        } else if (part instanceof ContentPartBase64) {
            return encodeBase64Responses((ContentPartBase64) part);
        }
        return part;
    }

    private static Object encodeBase64Responses(ContentPartBase64 part) {
        String base64Data = new String(part.getData());
        String contentFormat = part.getContentType().split("/")[1];
        if (part.getType() == MediaType.IMAGE) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_image");
            m.put("image_url", String.format("data:%s;base64,%s", part.getContentType(), base64Data));
            return m;
        } else if (part.getType() == MediaType.FILE) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "input_file");
            m.put("filename", String.format("%s.%s", part.getSlotName(), contentFormat));
            m.put("file_data", String.format("data:%s;base64,%s", part.getContentType(), base64Data));
            return m;
        } else if (part.getType() == MediaType.AUDIO) {
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
