package ai.freeplay.client.resources.prompts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

/**
 * Image URL content block matching the backend's SimpleMediaMessage schema.
 * Serializes to: { "type": "image_url", "url": "...", "media_type": "image" }
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ImageUrlContent {
    private final String type = "image_url";
    private final String url;
    private final String mediaType;

    public ImageUrlContent(String url, String mediaType) {
        this.url = url;
        this.mediaType = mediaType;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getMediaType() {
        return mediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ImageUrlContent that = (ImageUrlContent) o;
        return Objects.equals(url, that.url) && Objects.equals(mediaType, that.mediaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, mediaType);
    }

    @Override
    public String toString() {
        return "ImageUrlContent{type='image_url', url='" + url + "', mediaType='" + mediaType + "'}";
    }
}
