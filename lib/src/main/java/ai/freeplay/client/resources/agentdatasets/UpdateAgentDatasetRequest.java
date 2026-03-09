package ai.freeplay.client.resources.agentdatasets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAgentDatasetRequest {
    private String name;
    private String description;
    private List<String> compatibleAgentIds;
    private List<String> tags;

    public UpdateAgentDatasetRequest() {
    }

    public UpdateAgentDatasetRequest name(String name) {
        this.name = name;
        return this;
    }

    public UpdateAgentDatasetRequest description(String description) {
        this.description = description;
        return this;
    }

    public UpdateAgentDatasetRequest compatibleAgentIds(List<String> compatibleAgentIds) {
        this.compatibleAgentIds = compatibleAgentIds;
        return this;
    }

    public UpdateAgentDatasetRequest tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getCompatibleAgentIds() { return compatibleAgentIds; }
    public List<String> getTags() { return tags; }
}
