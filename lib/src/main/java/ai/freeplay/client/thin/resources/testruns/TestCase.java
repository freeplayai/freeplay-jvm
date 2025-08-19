package ai.freeplay.client.thin.resources.testruns;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * @deprecated Use {@link CompletionTestCase} instead. This class is maintained for backward compatibility only.
 * Migration: Replace TestCase with CompletionTestCase in your code, and use TestRun.getCompletionTestCases()
 * instead of the deprecated TestRun.getTestCases() method.
 */
@Deprecated
public class TestCase extends CompletionTestCase {
    public TestCase(String testCaseId, Map<String, Object> variables, String output, List<ChatMessage> history, Map<String, String> customMetadata) {
        super(testCaseId, variables, output, history, customMetadata);
    }
}
