package ai.freeplay.client;

import ai.freeplay.client.adapters.*;

import ai.freeplay.client.resources.prompts.AudioContent;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FileContent;
import ai.freeplay.client.resources.prompts.ImageContent;
import ai.freeplay.client.resources.prompts.TextContent;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class BedrockConverseAdapterTest {

    private final BedrockConverseAdapter adapter = new BedrockConverseAdapter();

    @Test
    public void testProviderName() {
        assertEquals("bedrock", adapter.getProvider());
    }

    @Test
    public void testTextOnlyContent() {
        ChatMessage systemMsg = new ChatMessage("system", "You are a helpful assistant");
        ChatMessage userMsg = new ChatMessage("user", "Hello");
        List<ChatMessage> messages = Arrays.asList(systemMsg, userMsg);

        List<Map<String, Object>> result = adapter.toLLMSyntax(messages);

        // System message should be filtered out
        assertEquals(1, result.size());
        assertEquals("user", result.get(0).get("role"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get(0).get("content");
        assertEquals(1, content.size());
        assertEquals("Hello", content.get(0).get("text"));
    }

    @Test
    public void testStructuredTextContent() {
        List<Object> structuredContent = new ArrayList<>();
        structuredContent.add(new TextContent("Analyze this"));

        ChatMessage message = new ChatMessage("user", structuredContent);
        List<ChatMessage> messages = Arrays.asList(message);

        List<Map<String, Object>> result = adapter.toLLMSyntax(messages);

        assertEquals(1, result.size());
        Map<String, Object> msg = result.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
        assertEquals(1, content.size());
        assertEquals("Analyze this", content.get(0).get("text"));
    }

    @Test
    public void testStructuredContentWithBase64Image() {
        List<Object> structuredContent = new ArrayList<>();
        structuredContent.add(new ImageContent(
                "image/png",
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
        ));

        ChatMessage message = new ChatMessage("user", structuredContent);
        List<ChatMessage> messages = Arrays.asList(message);

        List<Map<String, Object>> result = adapter.toLLMSyntax(messages);

        assertEquals(1, result.size());
        Map<String, Object> msg = result.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
        assertEquals(1, content.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> imageContent = content.get(0);
        assertTrue(imageContent.containsKey("image"));

        @SuppressWarnings("unchecked")
        Map<String, Object> image = (Map<String, Object>) imageContent.get("image");
        assertEquals("png", image.get("format"));

        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) image.get("source");
        assertTrue(source.containsKey("bytes"));

        byte[] bytes = (byte[]) source.get("bytes");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testStructuredContentWithDocument() {
        List<Object> structuredContent = new ArrayList<>();
        structuredContent.add(new FileContent(
                "application/pdf",
                "JVBERi0xLjQKJeLjz9M=",
                "test_doc"
        ));

        ChatMessage message = new ChatMessage("user", structuredContent);
        List<ChatMessage> messages = Arrays.asList(message);

        List<Map<String, Object>> result = adapter.toLLMSyntax(messages);

        assertEquals(1, result.size());
        Map<String, Object> msg = result.get(0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
        assertEquals(1, content.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> docContent = content.get(0);
        assertTrue(docContent.containsKey("document"));

        @SuppressWarnings("unchecked")
        Map<String, Object> document = (Map<String, Object>) docContent.get("document");
        assertEquals("pdf", document.get("format"));
        assertEquals("test_doc", document.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) document.get("source");
        assertTrue(source.containsKey("bytes"));
    }

    @Test(expected = ai.freeplay.client.exceptions.FreeplayConfigurationException.class)
    public void testUnsupportedAudioThrowsException() {
        List<Object> structuredContent = new ArrayList<>();
        structuredContent.add(new AudioContent("audio/mp3", "fake_audio_data"));

        ChatMessage message = new ChatMessage("user", structuredContent);
        List<ChatMessage> messages = Arrays.asList(message);

        adapter.toLLMSyntax(messages);
    }

    // Video content is not a supported type — no VideoContent class exists,
    // so it cannot be constructed through the SDK's bind() flow.
}
