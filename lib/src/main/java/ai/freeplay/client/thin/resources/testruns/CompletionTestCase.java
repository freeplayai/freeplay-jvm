package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;

import java.util.List;
import java.util.Map;

public class CompletionTestCase {
    private final String testCaseId;
    private final Map<String, Object> variables;
    private final String output;
    private final List<ChatMessage> history;
    private final Map<String, String> customMetadata;

    public CompletionTestCase(String testCaseId, Map<String, Object> variables, String output, List<ChatMessage> history, Map<String, String> customMetadata) {
        this.testCaseId = testCaseId;
        this.variables = variables;
        this.output = output;
        this.history = history;
        this.customMetadata = customMetadata;
    }

    @SuppressWarnings("unused")
    public CompletionTestCase(String testCaseId, Map<String, Object> variables, String output, List<ChatMessage> history) {
        this(testCaseId, variables, output, history, null);
    }

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

    @Override
    public String toString() {
        return "CompletionTestCase{" +
                "testCaseId='" + testCaseId + '\'' +
                ", variables=" + variables +
                ", output='" + output + '\'' +
                ", history=" + history +
                ", customMetadata=" + customMetadata +
                '}';
    }
}