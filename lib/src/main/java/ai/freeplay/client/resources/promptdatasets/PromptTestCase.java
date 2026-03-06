package ai.freeplay.client.resources.promptdatasets;

import ai.freeplay.client.resources.prompts.ChatMessage;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PromptTestCase {
    private String id;
    private Map<String, Object> inputs;
    private String output;
    private ChatMessage outputMessage;
    private Map<String, String> metadata;
    private Map<String, Object> mediaInputs;
    private List<ChatMessage> history;

    public PromptTestCase() {
    }

    public void setId(String id) { this.id = id; }
    public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }
    public void setOutput(String output) { this.output = output; }
    public void setOutputMessage(ChatMessage outputMessage) { this.outputMessage = outputMessage; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public void setMediaInputs(Map<String, Object> mediaInputs) { this.mediaInputs = mediaInputs; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }

    public String getId() { return id; }
    public Map<String, Object> getInputs() { return inputs; }
    public String getOutput() { return output; }
    public ChatMessage getOutputMessage() { return outputMessage; }
    public Map<String, String> getMetadata() { return metadata; }
    public Map<String, Object> getMediaInputs() { return mediaInputs; }
    public List<ChatMessage> getHistory() { return history; }
}
