package ai.freeplay.client.thin.resources.prompts;

import java.util.Arrays;
import java.util.Objects;

public class ContentPartBase64 implements ContentPart {
    private final String slotName;
    private final MediaType type;
    private final String contentType;
    private final byte[] data;

    public ContentPartBase64(String slotName, MediaType type, String contentType, byte[] data) {
        this.slotName = slotName;
        this.type = type;
        this.contentType = contentType;
        this.data = data;
    }

    public String getSlotName() {
        return slotName;
    }

    public MediaType getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ContentPartBase64 that = (ContentPartBase64) o;
        return Objects.equals(slotName, that.slotName) && type == that.type && Objects.equals(contentType, that.contentType) && Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotName, type, contentType, Arrays.hashCode(data));
    }

    @Override
    public String toString() {
        return "ContentPartBase64{" +
                "slotName='" + slotName + '\'' +
                ", type=" + type +
                ", contentType='" + contentType + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
