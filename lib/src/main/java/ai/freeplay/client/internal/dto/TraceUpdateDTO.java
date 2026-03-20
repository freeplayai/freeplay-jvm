package ai.freeplay.client.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceUpdateDTO {
    private Object output;
    private Map<String, Object> metadata;
    private Map<String, Object> feedback;
    private Map<String, Object> evalResults;
    private TestRunInfoDTO testRunInfo;

    public TraceUpdateDTO(
            Object output,
            Map<String, Object> metadata,
            Map<String, Object> feedback,
            Map<String, Object> evalResults,
            String testRunId,
            String testCaseId
    ) {
        this.output = output;
        this.metadata = metadata;
        this.feedback = feedback;
        this.evalResults = evalResults;
        if (testRunId != null && testCaseId != null) {
            this.testRunInfo = new TestRunInfoDTO(testRunId, testCaseId);
        }
    }

    @SuppressWarnings("unused")
    public Object getOutput() {
        return output;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getFeedback() {
        return feedback;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getEvalResults() {
        return evalResults;
    }

    @SuppressWarnings("unused")
    public TestRunInfoDTO getTestRunInfo() {
        return testRunInfo;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestRunInfoDTO {
        private final String testRunId;
        private final String testCaseId;

        public TestRunInfoDTO(String testRunId, String testCaseId) {
            this.testRunId = testRunId;
            this.testCaseId = testCaseId;
        }

        @SuppressWarnings("unused")
        public String getTestRunId() {
            return testRunId;
        }

        @SuppressWarnings("unused")
        public String getTestCaseId() {
            return testCaseId;
        }
    }
}
