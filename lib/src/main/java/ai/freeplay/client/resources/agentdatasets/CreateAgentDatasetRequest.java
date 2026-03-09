package ai.freeplay.client.resources.agentdatasets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAgentDatasetRequest {
    private final String name;
    private String description;
    private List<String> compatibleAgentIds;
    private List<String> tags;

    public CreateAgentDatasetRequest(String name) {
        this.name = name;
    }

    public CreateAgentDatasetRequest description(String description) {
        this.description = description;
        return this;
    }

    public CreateAgentDatasetRequest compatibleAgentIds(List<String> compatibleAgentIds) {
        this.compatibleAgentIds = compatibleAgentIds;
        return this;
    }

    public CreateAgentDatasetRequest tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getCompatibleAgentIds() { return compatibleAgentIds; }
    public List<String> getTags() { return tags; }
}
