package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.resources.datasets.Tag;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PromptDataset {
    private String id;
    private String name;
    private String description;
    private List<String> inputNames;
    private List<String> mediaInputNames;
    private boolean supportHistory;
    private List<Tag> tags;

    public PromptDataset() {
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setInputNames(List<String> inputNames) { this.inputNames = inputNames; }
    public void setMediaInputNames(List<String> mediaInputNames) { this.mediaInputNames = mediaInputNames; }
    public void setSupportHistory(boolean supportHistory) { this.supportHistory = supportHistory; }
    public void setTags(List<Tag> tags) { this.tags = tags; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getInputNames() { return inputNames; }
    public List<String> getMediaInputNames() { return mediaInputNames; }
    public boolean isSupportHistory() { return supportHistory; }
    public List<Tag> getTags() { return tags; }
}
