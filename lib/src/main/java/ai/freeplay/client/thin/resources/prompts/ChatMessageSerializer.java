package ai.freeplay.client.thin.resources.prompts;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ChatMessageSerializer extends JsonSerializer<ChatMessage> {
    @Override
    public void serialize(ChatMessage message, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
        if (message.isCompletionMessage()) {
            // For whole messages, directly write the content object
            Object content = message.getCompletionMessage();
            if (content instanceof Map) {
                // Spread the content map directly at the root level
                @SuppressWarnings("unchecked")
                Map<String, Object> contentMap = (Map<String, Object>) content;
                gen.writeStartObject();
                for (Map.Entry<String, Object> entry : contentMap.entrySet()) {
                    gen.writeObjectField(entry.getKey(), entry.getValue());
                }
                gen.writeEndObject();
            } else {
                // If content is not a map, write it as is
                provider.defaultSerializeValue(content, gen);
            }
        } else {
            // Normal chat message serialization
            gen.writeStartObject();
            gen.writeStringField("role", message.getRole());
            
            if (message.isEmptyMessage() || message.isStringMessage()) {
                gen.writeStringField("content", message.getContent());
            } else {
                String contentKey = "content";
                if (message.isGemini()) {
                    contentKey = "parts";
                }

                gen.writeObjectField(contentKey, message.getStructuredContent());
            }
            gen.writeEndObject();
        }
    }
}
