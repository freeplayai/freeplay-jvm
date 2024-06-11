package ai.freeplay.client.thin.internal.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TestRunDTO {
    private String testRunId;
    private List<TestCase> testCases;
    private String testRunDescription;
    private String testRunName;

    public String getTestRunId() {
        return testRunId;
    }

    public List<TestCase> getTestCases() {
        return testCases;
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

        public String getTestCaseId() {
            return testCaseId;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public String getOutput() {
            return output;
        }
    }
}
