package ai.freeplay.client.media;

import java.util.Arrays;
import java.util.Objects;

public class MediaInputBase64 implements MediaInput {
    private final byte[] data;
    private final String contentType;

    public MediaInputBase64(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MediaInputBase64 that = (MediaInputBase64) o;
        return Objects.deepEquals(data, that.data) && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(data), contentType);
    }
}
