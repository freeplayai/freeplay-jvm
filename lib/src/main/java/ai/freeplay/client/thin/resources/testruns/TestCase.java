package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;

import java.util.List;
import java.util.Map;

public class TestCase {
    private final String testCaseId;
    private final Map<String, Object> variables;
    private final String output;
    private final List<ChatMessage> history;

    public TestCase(String testCaseId, Map<String, Object> variables, String output, List<ChatMessage> history) {
        this.testCaseId = testCaseId;
        this.variables = variables;
        this.output = output;
        this.history = history;
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

    public List<ChatMessage> getHistory() {
        return history;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "testCaseId='" + testCaseId + '\'' +
                ", variables=" + variables + '\'' +
                ", output=" + output +
                ", history=" + history +
                '}';
    }
}
