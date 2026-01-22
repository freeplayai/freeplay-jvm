package ai.freeplay.client.thin.resources.prompts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonSerialize(using = ChatMessageSerializer.class)
@JsonDeserialize(using = ChatMessageDeserializer.class)
public class ChatMessage {
    private String role;
    private String content;
    private List<Object> structuredContent;
    private Object completionMessage;
    private List<MediaSlot> mediaSlots;
    private boolean isGemini = false;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.mediaSlots = List.of();
    }

    public ChatMessage(String role, String content, List<MediaSlot> mediaSlots) {
        this.role = role;
        this.content = content;
        this.mediaSlots = mediaSlots;
    }

    public ChatMessage(String role, List<Object> structuredContent) {
        this.role = role;
        this.structuredContent = structuredContent;
    }

    private ChatMessage(String role, List<Object> structuredContent, boolean isGemini) {
        this.role = role;
        this.structuredContent = structuredContent;
        this.isGemini = isGemini;
    }

    public static ChatMessage newForGemini(String role, List<Object> structuredContent) {
        return new ChatMessage(role, structuredContent, true);
    }

    public ChatMessage(Object completionMessage) {
        this.completionMessage = completionMessage;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        if (this.isEmptyMessage()) {
            return null;
        }

        if (this.isCompletionMessage()) {
            throw new IllegalStateException("Message is not a string, use getCompletionMessage() instead");
        }

        if (this.isStructuredMessage()) {
            throw new IllegalStateException("Message is not a string, use getStructuredContent() instead");
        }

        return content;
    }

    public List<Object> getStructuredContent() {
        if (this.isEmptyMessage()) {
            return null;
        }

        if (this.isStringMessage()) {
            throw new IllegalStateException("Message is not a list, use getContent() instead");
        }

        if (this.isCompletionMessage()) {
            throw new IllegalStateException("Message is not a list, use getCompletionMessage() instead");
        }

        return structuredContent;
    }

    public Object getCompletionMessage() {
        if (this.isEmptyMessage()) {
            return null;
        }

        if (this.isStringMessage()) {
            throw new IllegalStateException("Message is not a completion message, use getContent() instead");
        }

        if (this.isStructuredMessage()) {
            throw new IllegalStateException("Message is not a string, use getStructuredContent() instead");
        }

        return completionMessage;
    }

    public List<MediaSlot> getMediaSlots() {
        return mediaSlots;
    }

    @JsonIgnore
    public boolean isGemini() {
        return isGemini;
    }

    @JsonIgnore
    public boolean isKind() {
        return false;
    }

    @JsonIgnore
    public boolean isEmptyMessage() {
        return this.content == null && this.structuredContent == null && this.completionMessage == null;
    }

    @JsonIgnore
    public boolean isStringMessage() {
        return this.content != null;
    }

    @JsonIgnore
    public boolean isStructuredMessage() {
        return this.structuredContent != null;
    }

    @JsonIgnore
    public boolean isCompletionMessage() {
        return this.completionMessage != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(role, that.role) && Objects.equals(content, that.content) && Objects.equals(structuredContent, that.structuredContent) && Objects.equals(completionMessage, that.completionMessage) && Objects.equals(mediaSlots, that.mediaSlots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, structuredContent, completionMessage, mediaSlots);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", structuredContent=" + structuredContent +
                ", completionMessage=" + completionMessage +
                ", mediaSlots=" + mediaSlots +
                '}';
    }
}
