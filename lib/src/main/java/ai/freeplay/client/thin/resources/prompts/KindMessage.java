package ai.freeplay.client.thin.resources.prompts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KindMessage extends ChatMessage {
    private final String value;

    public KindMessage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean isKind() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KindMessage)) return false;
        if (!super.equals(o)) return false;
        KindMessage that = (KindMessage) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "KindMessage{" +
                "value='" + value + '\'' +
                '}';
    }
}

