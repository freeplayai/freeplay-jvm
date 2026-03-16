package ai.freeplay.client.resources.prompts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

/**
 * Image base64 content block matching the backend's SimpleMediaMessage schema.
 * Serializes to: { "type": "image", "content_type": "image/png", "data": "..." }
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ImageContent {
    private final String type = "image";
    private final String contentType;
    private final String data;

    public ImageContent(String contentType, String data) {
        this.contentType = contentType;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public String getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ImageContent that = (ImageContent) o;
        return Objects.equals(contentType, that.contentType) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, data);
    }

    @Override
    public String toString() {
        return "ImageContent{type='image', contentType='" + contentType + "', data='" + data + "'}";
    }
}
