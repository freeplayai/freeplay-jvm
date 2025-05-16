package ai.freeplay.client.thin.resources.prompts;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MediaSlot {
    private MediaType type;
    private String placeholderName;

    public MediaSlot() {
    }

    public MediaSlot(MediaType type, String placeholderName) {
        this.type = type;
        this.placeholderName = placeholderName;
    }

    public MediaType getType() {
        return type;
    }

    public String getPlaceholderName() {
        return placeholderName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MediaSlot mediaSlot = (MediaSlot) o;
        return Objects.equals(type, mediaSlot.type) && Objects.equals(placeholderName, mediaSlot.placeholderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, placeholderName);
    }

    @Override
    public String toString() {
        return "MediaSlot{" +
                "type='" + type + '\'' +
                ", placeholderName='" + placeholderName + '\'' +
                '}';
    }
}