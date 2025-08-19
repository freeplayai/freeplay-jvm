package ai.freeplay.client.thin.internal.dto;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TestRunDTO {
    private String testRunId;
    private List<TestCase> testCases;
    private List<TraceTestCaseDTO> traceTestCases;
    private String testRunDescription;
    private String testRunName;

    public String getTestRunId() {
        return testRunId;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public List<TraceTestCaseDTO> getTraceTestCases() {
        return traceTestCases;
    }

    public String getTestRunDescription() {
        return testRunDescription;
    }
    public String getTestRunName() {
        return testRunName;
    }

    @SuppressWarnings("unused")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TestCase {
        private String testCaseId;
        private Map<String, Object> variables;
        private String output;
        private List<ChatMessage> history;
        private Map<String, String> customMetadata;

        public String getTestCaseId() {
            return testCaseId;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public String getOutput() {
            return output;
        }

        public List<ChatMessage> getHistory() {
            return history;
        }

        public Map<String, String> getCustomMetadata() {
            return customMetadata;
        }
    }

    @SuppressWarnings("unused")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TraceTestCaseDTO {
        private String testCaseId;
        private String input;
        private String output;
        private Map<String, String> customMetadata;

        public String getTestCaseId() {
            return testCaseId;
        }

        public String getInput() {
            return input;
        }

        public String getOutput() {
            return output;
        }

        public Map<String, String> getCustomMetadata() {
            return customMetadata;
        }
    }
}
