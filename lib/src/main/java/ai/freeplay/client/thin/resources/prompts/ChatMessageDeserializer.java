package ai.freeplay.client.thin.resources.prompts;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDeserializer extends JsonDeserializer<ChatMessage> {
    @Override
    public ChatMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        String role = node.get("role").asText();
        JsonNode contentNode = node.get("content");
        JsonNode mediaNode = node.get("media_slots");
        List<MediaSlot> mediaSlots = new ArrayList<>();

        if (mediaNode != null && mediaNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) mediaNode;
            for (JsonNode item : arrayNode) {
                mediaSlots.add(p.getCodec().treeToValue(item, MediaSlot.class));
            }
        }

        // Handle null content
        if (contentNode.isNull()) {
            return new ChatMessage(role, null, mediaSlots);
        }

        // Handle array content
        if (contentNode.isArray()) {
            List<Object> structuredContent = new ArrayList<>();
            ArrayNode arrayNode = (ArrayNode) contentNode;
            for (JsonNode item : arrayNode) {
                structuredContent.add(p.getCodec().treeToValue(item, Object.class));
            }
            return new ChatMessage(role, structuredContent);
        }

        // Handle string content
        if (contentNode.isTextual()) {
            return new ChatMessage(role, contentNode.asText(), mediaSlots);
        }

        // Handle object content (treat as structured)
        List<Object> structuredContent = new ArrayList<>();
        structuredContent.add(p.getCodec().treeToValue(contentNode, Object.class));
        return new ChatMessage(role, structuredContent);
    }
}
