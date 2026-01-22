package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.TemplatePrompt;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.sessions.Session;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import ai.freeplay.client.thin.resources.testruns.TestRunRequest;
import ai.freeplay.client.thin.resources.testruns.TraceTestCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.callOpenAIWithTools;
import static java.lang.String.format;

/**
 * Example demonstrating how to use trace test cases with agent datasets.
 * This example shows:
 * 1. Creating a test run with an agent dataset (which returns TraceTestCase objects)
 * 2. Processing each trace test case
 * 3. Recording traces with test run information
 */
public class ThinTestRunWithAgentDatasetExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String apiRoot = System.getenv("FREEPLAY_API_URL");
        String baseUrl = format("%s/api", apiRoot);

        String openaiApiKey = System.getenv("OPENAI_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .baseUrl(baseUrl)
        );

        // Get the prompt template
        TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, "witty-question", "latest").get();

        // Create test run with agent dataset - this will return TraceTestCase objects
        TestRunRequest testRunRequest = fpClient.testRuns().createRequest(projectId, "agent-ds")
                .name("TR from JVM")
                .description("Every name deserves appreciation")
                .includeOutputs(true)
                .flavorName(templatePrompt.getPromptInfo().getFlavorName())
                .build();

        TestRun testRun = fpClient.testRuns().create(testRunRequest).get();
        System.out.println("Created test run: " + testRun.getTestRunId());

        // Process each trace test case
        for (TraceTestCase testCase : testRun.getTraceTestCases()) {
            System.out.println("Processing test case: " + testCase.getTestCaseId());
            System.out.println("Input: " + testCase.getInput());

            // Create session and trace
            Session session = fpClient.sessions().create();
            TraceInfo traceInfo = session.createTrace(testCase.getInput())
                    .agentName("agent 2")
                    .customMetadata(Map.of("test_case_type", "appreciation", "input_length", testCase.getInput().length()));

            // Bind the template with the input
            FormattedPrompt<List<ChatMessage>> formattedPrompt = templatePrompt
                    .bind(new TemplatePrompt.BindRequest(Map.of("question", testCase.getInput())))
                    .format();

            // Call OpenAI
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = callOpenAIWithTools(
                    objectMapper,
                    openaiApiKey,
                    formattedPrompt.getPromptInfo().getModel(),
                    formattedPrompt.getPromptInfo().getModelParameters(),
                    formattedPrompt.getFormattedPrompt(),
                    formattedPrompt.getToolSchema()
            ).get();
            long endTime = System.currentTimeMillis();

            // Parse the response
            String completion = null;
            try {
                JsonNode responseNode = objectMapper.readTree(response.body());
                JsonNode choicesNode = responseNode.get("choices");
                if (choicesNode != null && !choicesNode.isEmpty()) {
                    JsonNode messageNode = choicesNode.get(0).get("message");
                    if (messageNode != null) {
                        completion = messageNode.get("content").asText();
                    }
                }
            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse OpenAI response: " + e.getMessage());
                continue;
            }

            if (completion == null) {
                System.err.println("No completion found in response");
                continue;
            }

            System.out.println("Completion: " + completion);
            List<ChatMessage> allMessages = formattedPrompt.allMessages(
                    new ChatMessage("assistant", completion)
            );
            CallInfo callInfo = CallInfo.from(
                    formattedPrompt.getPromptInfo(),
                    startTime,
                    System.currentTimeMillis()
            );
            fpClient.recordings().create(
                    new RecordInfo(
                            projectId,
                            allMessages
                    ).inputs(Map.of("question", completion))
                            .promptVersionInfo(formattedPrompt.getPromptInfo())
                            .callInfo(callInfo)
                            .toolSchema(formattedPrompt.getToolSchema())
                            .testRunInfo(testRun.getTestRunInfo(testCase.getTestCaseId()))
                            .sessionInfo(session.getSessionInfo())
                            .traceInfo(traceInfo)
            ).get();


            // Record the trace output with test run information
            traceInfo.recordOutput(
                    projectId,
                    completion,
                    Map.of(
                            "f1_score", 0.48,
                            "is_non_empty", !completion.trim().isEmpty(),
                            "response_time_ms", endTime - startTime
                    ),
                    testRun.getTestRunInfo(testCase.getTestCaseId())
            ).get();

            System.out.println("Recorded trace for test case: " + testCase.getTestCaseId());
            System.out.println("---");
        }

        System.out.println("Completed processing " + testRun.getTraceTestCases().size() + " trace test cases");
    }
}