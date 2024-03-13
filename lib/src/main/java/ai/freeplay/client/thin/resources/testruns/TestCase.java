package ai.freeplay.client.thin.resources.testruns;

import java.util.Map;

public class TestCase {
    private final String testCaseId;
    private final Map<String, Object> variables;
    private final String output;

    public TestCase(String testCaseId, Map<String, Object> variables, String output) {
        this.testCaseId = testCaseId;
        this.variables = variables;
        this.output = output;
    }

    @SuppressWarnings("unused")
    public String getTestCaseId() {
        return testCaseId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public String getOutput() {
        return output;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "testCaseId='" + testCaseId + '\'' +
                ", variables=" + variables + '\'' +
                ", output=" + output +
                '}';
    }
}
