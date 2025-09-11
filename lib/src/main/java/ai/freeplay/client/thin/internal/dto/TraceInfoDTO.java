package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.thin.resources.recordings.TestRunInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TraceInfoDTO {
    private String input;
    private String output;
    private String agentName;
    private Map<String, Object> customMetadata;
    private Map<String, Object> evalResults;
    private RecordDTO.TestRunInfoDTO testRunInfo;
    private UUID parentId;

    @SuppressWarnings("unused")
    public TraceInfoDTO() {
    }


    public TraceInfoDTO(
            String input,
            String output,
            String agentName,
            Map<String, Object> customMetadata,
            Map<String, Object> evalResults,
            TestRunInfo testRunInfo,
            UUID parentId
    ) {
        this.input = input;
        this.output = output;
        this.agentName = agentName;
        this.customMetadata = customMetadata;
        this.evalResults = evalResults;
        this.testRunInfo = testRunInfo != null ? new RecordDTO.TestRunInfoDTO(testRunInfo.getTestRunId(), testRunInfo.getTestCaseId()) : null;
        this.parentId = parentId;
    }

    @SuppressWarnings("unused")
    public String getInput() {
        return input;
    }

    @SuppressWarnings("unused")
    public String getOutput() {
        return output;
    }

    @SuppressWarnings("unused")
    public String getAgentName() {
        return agentName;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    @SuppressWarnings("unused")
    public RecordDTO.TestRunInfoDTO getTestRunInfo() {
        return testRunInfo;
    }

    @SuppressWarnings("unused")
    public UUID getParentId() {
        return parentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfoDTO that = (TraceInfoDTO) o;
        return Objects.equals(input, that.input) && Objects.equals(output, that.output) && Objects.equals(agentName, that.agentName) && Objects.equals(customMetadata, that.customMetadata) && Objects.equals(evalResults, that.evalResults) && Objects.equals(parentId, that.parentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output, agentName, customMetadata, evalResults, parentId);
    }
}
