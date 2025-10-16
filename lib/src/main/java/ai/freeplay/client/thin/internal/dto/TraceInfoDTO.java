package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.thin.resources.recordings.TestRunInfo;
import ai.freeplay.client.thin.resources.sessions.SpanKind;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TraceInfoDTO {
    private Object input;
    private Object output;
    private String agentName;
    private Map<String, Object> customMetadata;
    private Map<String, Object> evalResults;
    private RecordDTO.TestRunInfoDTO testRunInfo;
    private UUID parentId;
    private String kind;
    private String name;
    private String startTime;
    private String endTime;

    @SuppressWarnings("unused")
    public TraceInfoDTO() {
    }


    public TraceInfoDTO(
            Object input,
            Object output,
            String agentName,
            Map<String, Object> customMetadata,
            Map<String, Object> evalResults,
            TestRunInfo testRunInfo,
            UUID parentId,
            SpanKind kind,
            String name,
            Instant startTime,
            Instant endTime
    ) {
        this.input = input;
        this.output = output;
        this.agentName = agentName;
        this.customMetadata = customMetadata;
        this.evalResults = evalResults;
        this.testRunInfo = testRunInfo != null ? new RecordDTO.TestRunInfoDTO(testRunInfo.getTestRunId(), testRunInfo.getTestCaseId()) : null;
        this.parentId = parentId;
        this.kind = kind != null ? kind.getValue() : null;
        this.name = name;
        this.startTime = startTime != null ? startTime.toString() : null;
        this.endTime = endTime != null ? endTime.toString() : null;
    }

    @SuppressWarnings("unused")
    public Object getInput() {
        return input;
    }

    @SuppressWarnings("unused")
    public Object getOutput() {
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

    @SuppressWarnings("unused")
    public String getKind() {
        return kind;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public String getStartTime() {
        return startTime;
    }

    @SuppressWarnings("unused")
    public String getEndTime() {
        return endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceInfoDTO that = (TraceInfoDTO) o;
        return Objects.equals(input, that.input) && Objects.equals(output, that.output) && Objects.equals(agentName, that.agentName) && Objects.equals(customMetadata, that.customMetadata) && Objects.equals(evalResults, that.evalResults) && Objects.equals(parentId, that.parentId) && Objects.equals(kind, that.kind) && Objects.equals(name, that.name) && Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output, agentName, customMetadata, evalResults, parentId, kind, name, startTime, endTime);
    }
}
