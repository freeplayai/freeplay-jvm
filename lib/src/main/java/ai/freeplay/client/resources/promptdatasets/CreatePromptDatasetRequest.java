package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.exceptions.FreeplayClientException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatePromptDatasetRequest {
    private final String name;
    private String description;
    private List<String> inputNames;
    private List<String> mediaInputNames;
    private boolean supportHistory = false;
    private List<String> tags;

    public CreatePromptDatasetRequest(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new FreeplayClientException("name must not be null or blank");
        }
        this.name = name;
    }

    public CreatePromptDatasetRequest description(String description) {
        this.description = description;
        return this;
    }

    public CreatePromptDatasetRequest inputNames(List<String> inputNames) {
        this.inputNames = inputNames;
        return this;
    }

    public CreatePromptDatasetRequest mediaInputNames(List<String> mediaInputNames) {
        this.mediaInputNames = mediaInputNames;
        return this;
    }

    public CreatePromptDatasetRequest supportHistory(boolean supportHistory) {
        this.supportHistory = supportHistory;
        return this;
    }

    public CreatePromptDatasetRequest tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getInputNames() { return inputNames; }
    public List<String> getMediaInputNames() { return mediaInputNames; }
    public boolean isSupportHistory() { return supportHistory; }
    public List<String> getTags() { return tags; }
}
