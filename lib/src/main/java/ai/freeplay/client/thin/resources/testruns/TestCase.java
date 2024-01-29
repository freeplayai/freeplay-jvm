package ai.freeplay.client.thin.resources.testruns;

import java.util.Map;

public class TestCase {
    private final String testCaseId;
    private final Map<String, Object> variables;

    public TestCase(String testCaseId, Map<String, Object> variables) {
        this.testCaseId = testCaseId;
        this.variables = variables;
    }

    @SuppressWarnings("unused")
    public String getTestCaseId() {
        return testCaseId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "testCaseId='" + testCaseId + '\'' +
                ", variables=" + variables +
                '}';
    }
}
