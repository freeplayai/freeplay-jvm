package ai.freeplay.client.resources.promptdatasets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdatePromptDatasetRequest {
    private String name;
    private String description;
    private List<String> inputNames;
    private List<String> mediaInputNames;
    private Boolean supportHistory;
    private List<String> tags;

    public UpdatePromptDatasetRequest() {
    }

    public UpdatePromptDatasetRequest name(String name) {
        this.name = name;
        return this;
    }

    public UpdatePromptDatasetRequest description(String description) {
        this.description = description;
        return this;
    }

    public UpdatePromptDatasetRequest inputNames(List<String> inputNames) {
        this.inputNames = inputNames;
        return this;
    }

    public UpdatePromptDatasetRequest mediaInputNames(List<String> mediaInputNames) {
        this.mediaInputNames = mediaInputNames;
        return this;
    }

    public UpdatePromptDatasetRequest supportHistory(boolean supportHistory) {
        this.supportHistory = supportHistory;
        return this;
    }

    public UpdatePromptDatasetRequest tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getInputNames() { return inputNames; }
    public List<String> getMediaInputNames() { return mediaInputNames; }
    public Boolean getSupportHistory() { return supportHistory; }
    public List<String> getTags() { return tags; }
}
