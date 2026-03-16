package ai.freeplay.client.resources.prompts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

/**
 * Audio content block matching the backend's SimpleMediaMessage schema.
 * Serializes to: { "type": "audio", "content_type": "audio/mpeg", "data": "..." }
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioContent {
    private final String type = "audio";
    private final String contentType;
    private final String data;

    public AudioContent(String contentType, String data) {
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
        AudioContent that = (AudioContent) o;
        return Objects.equals(contentType, that.contentType) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, data);
    }

    @Override
    public String toString() {
        return "AudioContent{type='audio', contentType='" + contentType + "', data='" + data + "'}";
    }
}
