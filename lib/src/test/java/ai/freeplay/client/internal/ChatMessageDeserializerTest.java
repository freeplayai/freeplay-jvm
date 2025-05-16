package ai.freeplay.client.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import ai.freeplay.client.thin.resources.prompts.MediaSlot;
import ai.freeplay.client.thin.resources.prompts.MediaType;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;

public class ChatMessageDeserializerTest {
    
    private ObjectMapper mapper;
    
    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }
    
    @Test
    public void testDeserializeStringMessage() throws IOException {
        String json = "{\"role\":\"user\",\"content\":\"Hello, world!\"}";
        
        ChatMessage message = mapper.readValue(json, ChatMessage.class);
        
        assertEquals("user", message.getRole());
        assertEquals("Hello, world!", message.getContent());
    }
    
    @Test
    public void testDeserializeStructuredContent() throws IOException {
        String json = "{\"role\":\"assistant\",\"content\":[{\"text\":\"Hello\"},{\"importance\":5}]}";
        
        ChatMessage message = mapper.readValue(json, ChatMessage.class);
        
        assertEquals("assistant", message.getRole());
        assertNotNull(message.getStructuredContent());
        List<?> content = (List<?>) message.getStructuredContent();
        assertEquals(2, content.size());
        assertEquals(Map.of("text", "Hello"), content.get(0));
        assertEquals(Map.of("importance", 5), content.get(1));
    }
    
    @Test
    public void testDeserializeNullContent() throws IOException {
        String json = "{\"role\":\"user\",\"content\":null}";
        
        ChatMessage message = mapper.readValue(json, ChatMessage.class);
        
        assertEquals("user", message.getRole());
        assertNull(message.getContent());
    }

    @Test
    public void testDeserializeMediaSlots() throws IOException {
        String json = "{\"role\":\"user\",\"content\":\"What do you see?\", \"media_slots\": [{\"type\": \"audio\", \"placeholder_name\": \"some-audio\"}]}";

        ChatMessage message = mapper.readValue(json, ChatMessage.class);

        assertEquals(List.of(new MediaSlot(MediaType.AUDIO, "some-audio")), message.getMediaSlots());
    }
}
