package ai.freeplay.client.resources.prompts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

/**
 * File content block matching the backend's SimpleMediaMessage schema.
 * Serializes to: { "type": "file", "content_type": "application/pdf", "data": "...", "filename": "name" }
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FileContent {
    private final String type = "file";
    private final String contentType;
    private final String data;
    private final String filename;

    public FileContent(String contentType, String data, String filename) {
        this.contentType = contentType;
        this.data = data;
        this.filename = filename;
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

    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileContent that = (FileContent) o;
        return Objects.equals(contentType, that.contentType) && Objects.equals(data, that.data) && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, data, filename);
    }

    @Override
    public String toString() {
        return "FileContent{type='file', contentType='" + contentType + "', data='" + data + "', filename='" + filename + "'}";
    }
}
