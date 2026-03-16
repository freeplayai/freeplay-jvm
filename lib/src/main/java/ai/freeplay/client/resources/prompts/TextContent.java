package ai.freeplay.client.resources.prompts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

/**
 * Text content block matching the backend's SimpleMediaMessage schema.
 * Serializes to: { "type": "text", "text": "..." }
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TextContent {
    private final String type = "text";
    private final String text;

    public TextContent(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TextContent that = (TextContent) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }

    @Override
    public String toString() {
        return "TextContent{type='text', text='" + text + "'}";
    }
}
