package ai.freeplay.client.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceUpdateDTO {
    private Object output;
    private Map<String, Object> evalResults;

    public TraceUpdateDTO(Object output, Map<String, Object> evalResults) {
        this.output = output;
        this.evalResults = evalResults;
    }

    @SuppressWarnings("unused")
    public Object getOutput() {
        return output;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getEvalResults() {
        return evalResults;
    }
}
