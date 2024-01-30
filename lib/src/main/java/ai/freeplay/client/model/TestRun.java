package ai.freeplay.client.model;

import ai.freeplay.client.internal.CallSupport;
import ai.freeplay.client.internal.ParameterUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestRun {
    private final CallSupport callSupport;
    private final String projectId;
    private final String environment;
    private final String testRunId;
    private final List<Map<String, Object>> inputs;

    public TestRun(
            CallSupport callSupport,
            String projectId,
            String environment,
            String testRunId,
            List<Map<String, Object>> inputs
    ) {
        this.callSupport = callSupport;
        this.projectId = projectId;
        this.environment = environment;
        this.testRunId = testRunId;
        this.inputs = inputs;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public List<Map<String, Object>> getInputs() {
        return inputs;
    }

    public CompletionSession createSession() {
        return createSession(Collections.emptyMap());
    }

    public CompletionSession createSession(Map<String, Object> metadata) {
        String sessionId = CallSupport.createSessionId();
        ParameterUtils.validateBasicMap(metadata);
        Collection<PromptTemplate> prompts = callSupport.getPrompts(projectId, environment);
        return new CompletionSession(callSupport, sessionId, prompts, environment, testRunId, metadata);
    }
}
