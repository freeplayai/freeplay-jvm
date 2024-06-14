package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TraceInfoDTO {
    private String input;
    private String output;

    @SuppressWarnings("unused")
    public TraceInfoDTO() {
    }

    public TraceInfoDTO (
            String input,
            String output
    ) {
        this.input = input;
        this.output = output;
    }

    @SuppressWarnings("unused")
    public String getInput(){return input;}
    @SuppressWarnings("unused")
    public String getOutput(){return output;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfoDTO that = (TraceInfoDTO) o;
        return Objects.equals(input, that.input) && Objects.equals(output, that.output);
    }

}
