package ai.freeplay.client.thin.resources.prompts;

import java.util.Objects;

public class ContentPartUrl implements ContentPart {
    private final String slotName;
    private final MediaType type;
    private final String url;

    public ContentPartUrl(String slotName, MediaType type, String url) {
        this.slotName = slotName;
        this.type = type;
        this.url = url;
    }

    public String getSlotName() {
        return slotName;
    }

    public MediaType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ContentPartUrl that = (ContentPartUrl) o;
        return Objects.equals(slotName, that.slotName) && Objects.equals(type, that.type) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotName, type, url);
    }

    @Override
    public String toString() {
        return "ContentPartUrl{" +
                "slotName='" + slotName + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
