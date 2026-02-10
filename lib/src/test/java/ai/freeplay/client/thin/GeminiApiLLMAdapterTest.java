package ai.freeplay.client.thin;

import ai.freeplay.client.thin.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.ContentPartBase64;
import ai.freeplay.client.thin.resources.prompts.ContentPartText;
import ai.freeplay.client.thin.resources.prompts.MediaType;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class GeminiApiLLMAdapterTest {

    @Test
    public void testGetProvider() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();
        assertEquals("gemini", adapter.getProvider());
    }

    @Test
    public void testAdapterForFlavorReturnsGeminiApiAdapter() {
        LLMAdapters.LLMAdapter<?> adapter = LLMAdapters.adapterForFlavor("gemini_api_chat");
        assertNotNull(adapter);
        assertTrue(adapter instanceof GeminiApiLLMAdapter);
    }

    @Test
    public void testToLLMSyntaxBasicMessages() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(
                new ChatMessage("system", "System prompt"),
                new ChatMessage("user", "Hello"),
                new ChatMessage("assistant", "Hi there")
        ));

        // System message should be filtered out
        assertEquals(2, result.size());

        // User message
        assertEquals("user", result.get(0).get("role"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userParts = (List<Map<String, Object>>) result.get(0).get("parts");
        assertEquals(1, userParts.size());
        assertEquals("Hello", userParts.get(0).get("text"));

        // Assistant -> model
        assertEquals("model", result.get(1).get("role"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modelParts = (List<Map<String, Object>>) result.get(1).get("parts");
        assertEquals(1, modelParts.size());
        assertEquals("Hi there", modelParts.get(0).get("text"));
    }

    @Test
    public void testToLLMSyntaxReturnsPlainMaps() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(
                new ChatMessage("user", "Hello")
        ));

        // Should be plain LinkedHashMap, not a protobuf Content
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof LinkedHashMap);
    }

    @Test
    public void testToLLMSyntaxWithStructuredContent() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();
        byte[] imageBytes = Base64.getEncoder().encode("image-data".getBytes());

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(
                new ChatMessage("user", List.of(
                        new ContentPartText("Look at this image"),
                        new ContentPartBase64("img1", MediaType.IMAGE, "image/png", imageBytes)
                ))
        ));

        assertEquals(1, result.size());
        assertEquals("user", result.get(0).get("role"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) result.get(0).get("parts");
        assertEquals(2, parts.size());
        assertEquals("Look at this image", parts.get(0).get("text"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inlineData = (Map<String, Object>) parts.get(1).get("inline_data");
        assertEquals("image/png", inlineData.get("mime_type"));
    }

    @Test
    public void testToLLMSyntaxGeminiPartsPassthrough() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        // Create a Gemini-format message (like history from previous turns)
        List<Object> geminiParts = List.of(
                new GeminiLLMAdapter.ContentPart("I can help with that")
        );
        ChatMessage geminiMsg = ChatMessage.newForGemini("model", geminiParts);

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(geminiMsg));

        assertEquals(1, result.size());
        assertEquals("model", result.get(0).get("role"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) result.get(0).get("parts");
        assertEquals(1, parts.size());
        assertEquals("I can help with that", parts.get(0).get("text"));
    }

    @Test
    public void testToLLMSyntaxGeminiPartsPassthroughWithInlineData() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        GeminiLLMAdapter.InlineDataContent inlineData =
                new GeminiLLMAdapter.InlineDataContent("image/png", "base64data");
        List<Object> geminiParts = List.of(
                new GeminiLLMAdapter.ContentPart(inlineData)
        );
        ChatMessage geminiMsg = ChatMessage.newForGemini("user", geminiParts);

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(geminiMsg));

        assertEquals(1, result.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) result.get(0).get("parts");
        assertEquals(1, parts.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> inlineDataMap = (Map<String, Object>) parts.get(0).get("inline_data");
        assertEquals("image/png", inlineDataMap.get("mime_type"));
        assertEquals("base64data", inlineDataMap.get("data"));
    }

    @Test
    public void testToLLMSyntaxAssistantRoleTranslatedToModel() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        List<Object> geminiParts = List.of(
                new GeminiLLMAdapter.ContentPart("Response text")
        );
        ChatMessage geminiMsg = ChatMessage.newForGemini("assistant", geminiParts);

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(geminiMsg));

        assertEquals("model", result.get(0).get("role"));
    }

    // ---- Tool Schema Tests ----

    @Test
    public void testToToolSchemaFormatSingleTool() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.of("location", Map.of("type", "string")));
        parameters.put("required", List.of("location"));

        List<Map<String, Object>> result = adapter.toToolSchemaFormat(List.of(
                new ToolSchema("get_weather", "Get weather info", parameters)
        ));

        // Single Tool object with functionDeclarations
        assertEquals(1, result.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> functionDeclarations =
                (List<Map<String, Object>>) result.get(0).get("functionDeclarations");
        assertEquals(1, functionDeclarations.size());
        assertEquals("get_weather", functionDeclarations.get(0).get("name"));
        assertEquals("Get weather info", functionDeclarations.get(0).get("description"));
        assertEquals(parameters, functionDeclarations.get(0).get("parameters"));
    }

    @Test
    public void testToToolSchemaFormatMultipleToolsGrouped() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        List<Map<String, Object>> result = adapter.toToolSchemaFormat(List.of(
                new ToolSchema("get_weather", "Get weather", Map.of()),
                new ToolSchema("get_time", "Get time", Map.of())
        ));

        // Still a single Tool with multiple functionDeclarations
        assertEquals(1, result.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> functionDeclarations =
                (List<Map<String, Object>>) result.get(0).get("functionDeclarations");
        assertEquals(2, functionDeclarations.size());
        assertEquals("get_weather", functionDeclarations.get(0).get("name"));
        assertEquals("get_time", functionDeclarations.get(1).get("name"));
    }

    @Test
    public void testToToolSchemaReturnsPlainMaps() {
        GeminiApiLLMAdapter adapter = new GeminiApiLLMAdapter();

        List<Map<String, Object>> result = adapter.toToolSchemaFormat(List.of(
                new ToolSchema("fn", "desc", Map.of())
        ));

        // All returned objects should be plain Maps, not protobuf objects
        assertTrue(result.get(0) instanceof LinkedHashMap);
    }
}
