package ai.freeplay.client.thin.resources.prompts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonSerialize(using = ChatMessageSerializer.class)
public class ChatMessage {
    private String role;
    private String content;
    private List<Object> structuredContent;
    private Object completionMessage;

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatMessage(String role, List<Object> structuredContent) {
        this.role = role;
        this.structuredContent = structuredContent;
    }

    public ChatMessage(Object completionMessage) {
        this.completionMessage = completionMessage;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        if (completionMessage != null) {
            throw new IllegalStateException("Message is not a string, use getWholeMessage() instead");
        }

        if (structuredContent != null) {
            throw new IllegalStateException("Message is not a string, use getStructuredContent() instead");
        }

        return content;
    }

    public List<Object> getStructuredContent() {
        if (completionMessage != null) {
            throw new IllegalStateException("Message is not a list, use getWholeMessage() instead");
        }

        if (content != null) {
            throw new IllegalStateException("Message is not a list, use getContent() instead");
        }

        return structuredContent;
    }

    public Object getCompletionMessage() {
        if (content != null) {
            throw new IllegalStateException("Message is not a string, use getContent() instead");
        }

        if (structuredContent != null) {
            throw new IllegalStateException("Message is not a string, use getStructuredContent() instead");
        }

        return completionMessage;
    }

    @JsonIgnore
    public boolean isKind() {
        return false;
    }

    protected boolean isStringMessage() {
        return content != null;
    }

    protected boolean isStructuredMessage() {
        return structuredContent != null;
    }

    protected boolean isCompletionMessage() {
        return completionMessage != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(role, that.role)
                && Objects.equals(content, that.content)
                && Objects.equals(structuredContent, that.structuredContent)
                && Objects.equals(completionMessage, that.completionMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, structuredContent, completionMessage);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", structuredContent='" + structuredContent + '\'' +
                ", completionMessage='" + completionMessage + '\'' +
                '}';
    }
}
