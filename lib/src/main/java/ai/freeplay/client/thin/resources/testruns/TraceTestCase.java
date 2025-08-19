package ai.freeplay.client.thin.resources.testruns;

import java.util.Map;

public class TraceTestCase {
    private final String testCaseId;
    private final String input;
    private final String output;
    private final Map<String, String> customMetadata;

    public TraceTestCase(String testCaseId, String input, String output, Map<String, String> customMetadata) {
        this.testCaseId = testCaseId;
        this.input = input;
        this.output = output;
        this.customMetadata = customMetadata;
    }

    @SuppressWarnings("unused")
    public TraceTestCase(String testCaseId, String input, String output) {
        this(testCaseId, input, output, null);
    }

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

    @Override
    public String toString() {
        return "TraceTestCase{" +
                "testCaseId='" + testCaseId + '\'' +
                ", input='" + input + '\'' +
                ", output='" + output + '\'' +
                ", customMetadata=" + customMetadata +
                '}';
    }
}