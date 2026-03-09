package ai.freeplay.client.internal.dto;

import ai.freeplay.client.resources.prompts.ChatMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BulkCreatePromptTestCasesDTO {
    private final List<TestCaseDTO> data;

    public BulkCreatePromptTestCasesDTO(List<TestCaseDTO> data) {
        this.data = data;
    }

    public List<TestCaseDTO> getData() {
        return data;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestCaseDTO {
        private final Map<String, Object> inputs;
        private final String output;
        private final Map<String, String> metadata;
        private final List<ChatMessage> history;
        private final Map<String, RecordDTO.MediaInputDTO> mediaInputs;

        public TestCaseDTO(
                Map<String, Object> inputs,
                String output,
                Map<String, String> metadata,
                List<ChatMessage> history,
                Map<String, RecordDTO.MediaInputDTO> mediaInputs
        ) {
            this.inputs = inputs;
            this.output = output;
            this.metadata = metadata;
            this.history = history;
            this.mediaInputs = mediaInputs;
        }

        public Map<String, Object> getInputs() { return inputs; }
        public String getOutput() { return output; }
        public Map<String, String> getMetadata() { return metadata; }
        public List<ChatMessage> getHistory() { return history; }
        public Map<String, RecordDTO.MediaInputDTO> getMediaInputs() { return mediaInputs; }
    }
}
