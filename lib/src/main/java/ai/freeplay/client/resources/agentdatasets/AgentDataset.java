package ai.freeplay.client.resources.agentdatasets;

import ai.freeplay.client.resources.datasets.Tag;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgentDataset {
    private String id;
    private String name;
    private String description;
    private List<String> compatibleAgentIds;
    private List<Tag> tags;

    public AgentDataset() {
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCompatibleAgentIds(List<String> compatibleAgentIds) { this.compatibleAgentIds = compatibleAgentIds; }
    public void setTags(List<Tag> tags) { this.tags = tags; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getCompatibleAgentIds() { return compatibleAgentIds; }
    public List<Tag> getTags() { return tags; }
}
