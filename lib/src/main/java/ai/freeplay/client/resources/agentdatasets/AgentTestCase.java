package ai.freeplay.client.resources.agentdatasets;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgentTestCase {
    private String id;
    private Object input;
    private Object output;
    private Map<String, String> metadata;

    public AgentTestCase() {
    }

    public void setId(String id) { this.id = id; }
    public void setInput(Object input) { this.input = input; }
    public void setOutput(Object output) { this.output = output; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public String getId() { return id; }
    public Object getInput() { return input; }
    public Object getOutput() { return output; }
    public Map<String, String> getMetadata() { return metadata; }
}
