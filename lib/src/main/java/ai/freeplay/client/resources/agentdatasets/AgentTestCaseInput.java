package ai.freeplay.client.resources.agentdatasets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentTestCaseInput {
    private Object inputs;
    private Object outputs;
    private Map<String, String> metadata;

    public AgentTestCaseInput() {
    }

    public AgentTestCaseInput inputs(Object inputs) {
        this.inputs = inputs;
        return this;
    }

    public AgentTestCaseInput outputs(Object outputs) {
        this.outputs = outputs;
        return this;
    }

    public AgentTestCaseInput metadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Object getInputs() { return inputs; }
    public Object getOutputs() { return outputs; }
    public Map<String, String> getMetadata() { return metadata; }
}
