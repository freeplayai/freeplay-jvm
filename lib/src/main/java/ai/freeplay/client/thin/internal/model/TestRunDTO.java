package ai.freeplay.client.thin.internal.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

// TODO should we adopt the DTO naming across the board?
@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TestRunDTO {
    private String testRunId;
    private List<TestCase> testCases;

    public String getTestRunId() {
        return testRunId;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    @SuppressWarnings("unused")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TestCase {
        private String id;
        private Map<String, Object> variables;

        public String getId() {
            return id;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }
    }
}
