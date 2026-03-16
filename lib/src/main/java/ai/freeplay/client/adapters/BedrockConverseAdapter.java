package ai.freeplay.client.adapters;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.v2dto.TemplateDTO;
import ai.freeplay.client.resources.prompts.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Adapter for AWS Bedrock Converse API format.
 * Handles conversion of Freeplay messages to Bedrock Converse format.
 */
public class BedrockConverseAdapter implements LLMAdapters.LLMAdapter<List<Map<String, Object>>> {

    @Override
    public String getProvider() {
        return "bedrock";
    }

    @Override
    public List<Map<String, Object>> toLLMSyntax(List<ChatMessage> messages) {
        return messages.stream()
                .filter(msg -> !isSystemMessage(msg))
                .map(this::convertMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> toToolSchemaFormat(List<TemplateDTO.ToolSchema> toolSchema) {
        if (toolSchema == null) {
            return null;
        }

        return toolSchema.stream()
                .filter(schema -> schema.getName() != null
                        && schema.getDescription() != null
                        && schema.getParameters() != null)
                .map(schema -> Map.of(
                        "name", schema.getName(),
                        "description", schema.getDescription(),
                        "inputSchema", schema.getParameters()
                ))
                .collect(toList());
    }

    private boolean isSystemMessage(ChatMessage message) {
        return "system".equals(message.getRole());
    }

    private Map<String, Object> convertMessage(ChatMessage message) {
        Map<String, Object> result = new HashMap<>();
        result.put("role", message.getRole());

        // Handle different content types
        if (message.isStringMessage()) {
            // Simple string content
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("text", message.getContent());
            content.add(textContent);
            result.put("content", content);
        } else if (message.isStructuredMessage()) {
            // Structured content with potential media
            List<Object> convertedContent = new ArrayList<>();
            for (Object item : message.getStructuredContent()) {
                convertedContent.add(convertContentItem(item));
            }
            result.put("content", convertedContent);
        } else if (message.isCompletionMessage()) {
            // Handle completion message - pass through as-is
            Object completionMsg = message.getCompletionMessage();
            if (completionMsg instanceof Map) {
                //noinspection unchecked
                return (Map<String, Object>) completionMsg;
            }
            result.put("content", completionMsg);
        }

        return result;
    }

    private Object convertContentItem(Object item) {
        if (item instanceof TextContent) {
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("text", ((TextContent) item).getText());
            return textContent;
        } else if (item instanceof ImageContent) {
            ImageContent img = (ImageContent) item;
            String format = extractFormat(img.getContentType());
            Map<String, Object> imageContent = new HashMap<>();
            Map<String, Object> image = new HashMap<>();
            image.put("format", format);
            Map<String, Object> source = new HashMap<>();
            source.put("bytes", Base64.getDecoder().decode(img.getData()));
            image.put("source", source);
            imageContent.put("image", image);
            return imageContent;
        } else if (item instanceof ImageUrlContent) {
            throw new FreeplayConfigurationException(
                "Bedrock Converse does not support URL-based media content"
            );
        } else if (item instanceof FileContent) {
            FileContent file = (FileContent) item;
            String format = extractFormat(file.getContentType());
            Map<String, Object> documentContent = new HashMap<>();
            Map<String, Object> document = new HashMap<>();
            document.put("format", format);
            document.put("name", file.getFilename());
            Map<String, Object> source = new HashMap<>();
            source.put("bytes", Base64.getDecoder().decode(file.getData()));
            document.put("source", source);
            documentContent.put("document", document);
            return documentContent;
        } else if (item instanceof AudioContent) {
            throw new FreeplayConfigurationException(
                "Bedrock Converse does not support audio content"
            );
        }

        // Return as-is for other types
        return item;
    }

    private String extractFormat(String contentType) {
        if (contentType == null) {
            return "jpeg"; // default
        }
        // Extract format from content type (e.g., "image/png" -> "png", "application/pdf" -> "pdf")
        String[] parts = contentType.split("/");
        if (parts.length > 1) {
            // Remove any parameters after semicolon (e.g., "png; charset=utf-8" -> "png")
            return parts[1].split(";")[0].trim();
        }
        return contentType;
    }
}