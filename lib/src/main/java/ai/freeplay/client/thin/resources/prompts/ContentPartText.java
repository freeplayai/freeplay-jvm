package ai.freeplay.client.thin.resources.prompts;

import java.util.Objects;

public class ContentPartText implements ContentPart {
    private final String text;

    public ContentPartText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ContentPartText that = (ContentPartText) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }

    @Override
    public String toString() {
        return "ContentPartText{" +
                "text='" + text + '\'' +
                '}';
    }
}
