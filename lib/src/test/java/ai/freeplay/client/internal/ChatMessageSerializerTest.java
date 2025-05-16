package ai.freeplay.client.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;

public class ChatMessageSerializerTest {
    
    private ObjectMapper mapper;
    
    @Before
    public void setUp() {
        mapper = new ObjectMapper();
    }
    
    @Test
    public void testSerializeStringMessage() throws IOException {
        ChatMessage message = new ChatMessage("user", "Hello, world!", List.of());
        
        String json = mapper.writeValueAsString(message);
        
        assertEquals("{\"role\":\"user\",\"content\":\"Hello, world!\"}", json);
    }
    
    @Test
    public void testSerializeStructuredContent() throws IOException {
        List<Object> content = new ArrayList<>();
        content.add(Map.of("text", "Hello"));
        content.add(Map.of("importance", 5));
        ChatMessage message = new ChatMessage("assistant", content);
        
        String json = mapper.writeValueAsString(message);
        
        assertEquals("{\"role\":\"assistant\",\"content\":[{\"text\":\"Hello\"},{\"importance\":5}]}", json);
    }
    
    @Test
    public void testSerializeCompletionMessageWithMap() throws IOException {
        Map<String, Object> completion = new HashMap<>();
        completion.put("finish_reason", "stop");
        completion.put("index", 0);
        ChatMessage message = new ChatMessage(completion);
        
        String json = mapper.writeValueAsString(message);
        
        assertEquals("{\"finish_reason\":\"stop\",\"index\":0}", json);
    }
    
    @Test
    public void testSerializeCompletionMessageWithNonMap() throws IOException {
        String completion = "Simple completion";
        ChatMessage message = new ChatMessage(completion);
        
        String json = mapper.writeValueAsString(message);
        
        assertEquals("\"Simple completion\"", json);
    }
    
    @Test
    public void testSerializeNullContent() throws IOException {
        ChatMessage message = new ChatMessage("user", null, List.of());
        
        String json = mapper.writeValueAsString(message);
        
        assertEquals("{\"role\":\"user\",\"content\":null}", json);
    }
}