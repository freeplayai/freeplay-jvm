package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.media.MediaInput;
import ai.freeplay.client.resources.prompts.ChatMessage;

import java.util.List;
import java.util.Map;

public class UpdatePromptTestCaseRequest {
    private Map<String, Object> inputs;
    private String output;
    private Map<String, String> metadata;
    private List<ChatMessage> history;
    private Map<String, MediaInput> mediaInputs;

    public UpdatePromptTestCaseRequest() {
    }

    public UpdatePromptTestCaseRequest inputs(Map<String, Object> inputs) {
        this.inputs = inputs;
        return this;
    }

    public UpdatePromptTestCaseRequest output(String output) {
        this.output = output;
        return this;
    }

    public UpdatePromptTestCaseRequest metadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public UpdatePromptTestCaseRequest history(List<ChatMessage> history) {
        this.history = history;
        return this;
    }

    public UpdatePromptTestCaseRequest mediaInputs(Map<String, MediaInput> mediaInputs) {
        this.mediaInputs = mediaInputs;
        return this;
    }

    public Map<String, Object> getInputs() { return inputs; }
    public String getOutput() { return output; }
    public Map<String, String> getMetadata() { return metadata; }
    public List<ChatMessage> getHistory() { return history; }
    public Map<String, MediaInput> getMediaInputs() { return mediaInputs; }
}
