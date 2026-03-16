package ai.freeplay.client.adapters;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.resources.prompts.*;

import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * LLM adapter for the Gemini API (google.genai SDK).
 * Returns plain Maps instead of Vertex AI protobuf Content objects,
 * so the output is directly usable with the google-genai client library.
 */
public class GeminiApiLLMAdapter implements LLMAdapters.LLMAdapter<List<Map<String, Object>>> {
    @Override
    public String getProvider() {
        return "gemini";
    }

    @Override
    public List<Map<String, Object>> toLLMSyntax(List<ChatMessage> messages) {
        return messages
                .stream()
                .filter(message -> !message.getRole().equals("system"))
                .map(message -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("role", translateRole(message.getRole()));

                    // Already in Gemini format (e.g., history from previous turns
                    // with function calls, function responses, or multipart content)
                    if (message.isGemini()) {
                        List<Map<String, Object>> parts = message.getStructuredContent().stream().map(item -> {
                            Map<String, Object> part = new LinkedHashMap<>();
                            if (item instanceof GeminiLLMAdapter.ContentPart) {
                                GeminiLLMAdapter.ContentPart cp = (GeminiLLMAdapter.ContentPart) item;
                                if (cp.getText() != null) {
                                    part.put("text", cp.getText());
                                } else if (cp.getInlineData() != null) {
                                    Map<String, Object> inlineData = new LinkedHashMap<>();
                                    inlineData.put("mime_type", cp.getInlineData().getMimeType());
                                    inlineData.put("data", cp.getInlineData().getData());
                                    part.put("inline_data", inlineData);
                                }
                            }
                            return part;
                        }).collect(toList());
                        result.put("parts", parts);
                    } else if (message.isStringMessage()) {
                        Map<String, Object> textPart = new LinkedHashMap<>();
                        textPart.put("text", message.getContent());
                        result.put("parts", Collections.singletonList(textPart));
                    } else if (message.isStructuredMessage()) {
                        List<Map<String, Object>> parts = message.getStructuredContent().stream().map(item -> {
                            Map<String, Object> part = new LinkedHashMap<>();
                            if (item instanceof TextContent) {
                                part.put("text", ((TextContent) item).getText());
                            } else if (item instanceof ImageUrlContent) {
                                throw new IllegalStateException("Message contains a media URL, but media URLs are not supported by Gemini");
                            } else if (item instanceof ImageContent) {
                                ImageContent img = (ImageContent) item;
                                Map<String, Object> inlineData = new LinkedHashMap<>();
                                inlineData.put("mime_type", img.getContentType());
                                inlineData.put("data", img.getData());
                                part.put("inline_data", inlineData);
                            } else if (item instanceof AudioContent) {
                                AudioContent audio = (AudioContent) item;
                                Map<String, Object> inlineData = new LinkedHashMap<>();
                                inlineData.put("mime_type", audio.getContentType());
                                inlineData.put("data", audio.getData());
                                part.put("inline_data", inlineData);
                            } else if (item instanceof FileContent) {
                                FileContent file = (FileContent) item;
                                Map<String, Object> inlineData = new LinkedHashMap<>();
                                inlineData.put("mime_type", file.getContentType());
                                inlineData.put("data", file.getData());
                                part.put("inline_data", inlineData);
                            }
                            return part;
                        }).collect(toList());
                        result.put("parts", parts);
                    } else {
                        throw new FreeplayConfigurationException(format("Unknown message for Gemini: %s", message));
                    }

                    return result;
                })
                .collect(toList());
    }

    @Override
    public List<Map<String, Object>> toToolSchemaFormat(List<ToolSchema> toolSchema) {
        List<Map<String, Object>> functionDeclarations = toolSchema.stream()
                .map(schema -> {
                    Map<String, Object> decl = new LinkedHashMap<>();
                    decl.put("name", schema.getName());
                    decl.put("description", schema.getDescription());
                    decl.put("parameters", schema.getParameters());
                    return decl;
                })
                .collect(toList());

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("functionDeclarations", functionDeclarations);
        return Collections.singletonList(tool);
    }

    private String translateRole(String role) {
        switch (role) {
            case "user":
                return "user";
            case "assistant":
            case "model":
                return "model";
            default:
                throw new FreeplayConfigurationException(
                        format("Unknown role in prompt template for Gemini: %s", role)
                );
        }
    }
}
