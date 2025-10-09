package ai.freeplay.client.thin;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
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
    public void testSimpleTextMessage() {
        // Create a simple text message
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("user", "Hello, world!")
        );

        List<Map<String, Object>> result = adapter.toLLMSyntax(messages);

        assertEquals(1, result.size());
        Map<String, Object> msg = result.get(0);
        assertEquals("user", msg.get("role"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
        assertEquals(1, content.size());
        assertEquals("Hello, world!", content.get(0).get("text"));
    }

    @Test
    public void testFiltersSystemMessages() {
        // Create messages including a system message
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("system", "You are a helpful assistant"),
            new ChatMessage("user", "Hello")
        );

        List<Map<String, Object>> result = adapter.toLLMSyntax(messages);

        // System message should be filtered out
        assertEquals(1, result.size());
        assertEquals("user", result.get(0).get("role"));
    }

    @Test
    public void testStructuredContentWithText() {
        // Create structured content message
        List<Object> structuredContent = new ArrayList<>();
        Map<String, Object> textItem = new HashMap<>();
        textItem.put("text", "Analyze this");
        structuredContent.add(textItem);

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
        // Create structured content with base64 encoded image
        List<Object> structuredContent = new ArrayList<>();

        Map<String, Object> imageItem = new HashMap<>();
        imageItem.put("slot_type", "image");
        imageItem.put("content_type", "image/png");
        imageItem.put("data", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        structuredContent.add(imageItem);

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

        // Verify bytes were decoded from base64
        byte[] bytes = (byte[]) source.get("bytes");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testStructuredContentWithDocument() {
        // Create structured content with document
        List<Object> structuredContent = new ArrayList<>();

        Map<String, Object> docItem = new HashMap<>();
        docItem.put("slot_type", "file");
        docItem.put("slot_name", "test_doc");
        docItem.put("content_type", "application/pdf");
        docItem.put("data", "JVBERi0xLjQKJeLjz9M="); // Minimal PDF base64
        structuredContent.add(docItem);

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

        Map<String, Object> audioItem = new HashMap<>();
        audioItem.put("slot_type", "audio");
        audioItem.put("content_type", "audio/mp3");
        audioItem.put("data", "fake_audio_data");
        structuredContent.add(audioItem);

        ChatMessage message = new ChatMessage("user", structuredContent);
        List<ChatMessage> messages = Arrays.asList(message);

        adapter.toLLMSyntax(messages); // Should throw exception
    }

    @Test(expected = ai.freeplay.client.exceptions.FreeplayConfigurationException.class)
    public void testUnsupportedVideoThrowsException() {
        List<Object> structuredContent = new ArrayList<>();

        Map<String, Object> videoItem = new HashMap<>();
        videoItem.put("slot_type", "video");
        videoItem.put("content_type", "video/mp4");
        videoItem.put("data", "fake_video_data");
        structuredContent.add(videoItem);

        ChatMessage message = new ChatMessage("user", structuredContent);
        List<ChatMessage> messages = Arrays.asList(message);

        adapter.toLLMSyntax(messages); // Should throw exception
    }
}