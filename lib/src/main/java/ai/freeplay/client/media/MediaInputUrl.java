package ai.freeplay.client.media;

import java.util.Objects;

public class MediaInputUrl implements MediaInput {
    private final String url;

    public MediaInputUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MediaInputUrl that = (MediaInputUrl) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }
}
