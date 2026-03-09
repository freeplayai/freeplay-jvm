package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.media.MediaInput;
import ai.freeplay.client.resources.prompts.ChatMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptTestCaseInput {
    private final Map<String, Object> inputs;
    private String output;
    private Map<String, String> metadata;
    private List<ChatMessage> history;
    private Map<String, MediaInput> mediaInputs;

    public PromptTestCaseInput(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public PromptTestCaseInput output(String output) {
        this.output = output;
        return this;
    }

    public PromptTestCaseInput metadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public PromptTestCaseInput history(List<ChatMessage> history) {
        this.history = history;
        return this;
    }

    public PromptTestCaseInput mediaInputs(Map<String, MediaInput> mediaInputs) {
        this.mediaInputs = mediaInputs;
        return this;
    }

    public Map<String, Object> getInputs() { return inputs; }
    public String getOutput() { return output; }
    public Map<String, String> getMetadata() { return metadata; }
    public List<ChatMessage> getHistory() { return history; }
    public Map<String, MediaInput> getMediaInputs() { return mediaInputs; }
}
