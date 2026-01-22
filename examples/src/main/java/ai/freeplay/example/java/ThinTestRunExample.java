package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.TemplatePrompt;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.sessions.Session;
import ai.freeplay.client.thin.resources.testruns.CompletionTestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import ai.freeplay.client.thin.resources.testruns.TestRunRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.callAnthropic;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class ThinTestRunExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String apiRoot = System.getenv("FREEPLAY_API_URL");
        String baseUrl = format("%s/api", apiRoot);

        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .baseUrl(baseUrl)
        );
        String testRunName = "Test run: " + UUID.randomUUID();

        TestRunRequest testRunRequest = fpClient.testRuns().createRequest(projectId, "core-tests")
                .name(testRunName)
                .description("Run from JVM examples")
                .build();

        List<RecordResponse> recordResponses = fpClient.testRuns().create(testRunRequest).thenCompose(testRun ->
                        fpClient.prompts().get(projectId, "my-anthropic-prompt", "latest")
                                .thenCompose(templatePrompt -> {
                                    var futures =
                                            testRun.getTestCases().stream()
                                                    .map(testCase ->
                                                            handleTestCase(
                                                                    fpClient,
                                                                    projectId,
                                                                    anthropicApiKey,
                                                                    templatePrompt,
                                                                    testRun,
                                                                    testCase))
                                                    .collect(toList());

                                    // It's not obvious why we have to go over the futures again after calling CompletableFuture.allOf().
                                    // The allOf call can only return Void, so it is only telling us when the futures have completed, not
                                    // what their result is. The join() call gets their result. It should not block or take a long time,
                                    // because it is only called after we've gotten the callback that all the futures completed.
                                    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                                            .thenApply((Void v) -> futures.stream().map(CompletableFuture::join).collect(toList()));
                                })
                ).join();

        System.out.println("Record responses: " + recordResponses);
    }

    private static CompletableFuture<RecordResponse> handleTestCase(
            Freeplay fpClient,
            String projectId,
            String anthropicApiKey,
            TemplatePrompt templatePrompt,
            TestRun testRun,
            CompletionTestCase testCase
    ) {
        FormattedPrompt<List<ChatMessage>> formattedPrompt =
                templatePrompt.bind(new TemplatePrompt.BindRequest(testCase.getVariables()).history(testCase.getHistory())).format();

        long startTime = System.currentTimeMillis();
        Session session = fpClient.sessions().create();
        return callAnthropic(
                objectMapper,
                anthropicApiKey,
                formattedPrompt.getPromptInfo().getModel(),
                formattedPrompt.getPromptInfo().getModelParameters(),
                formattedPrompt.getFormattedPrompt(),
                formattedPrompt.getSystemContent().orElse(null),
                formattedPrompt.getToolSchema()
        ).thenCompose((HttpResponse<String> response) ->
                recordAnthropic(fpClient, projectId, testRun, session, testCase, formattedPrompt, startTime, response)
                .thenCompose(recordResponse -> {
                    JsonNode firstBodyNode;
                    try {
                        firstBodyNode = objectMapper.readTree(response.body());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Unable to parse first response body.", e);
                    }

                    List<ChatMessage> messages = formattedPrompt.getFormattedPrompt();
                    // Add tool response message if there was a tool call
                    if (firstBodyNode.has("tool_calls") && firstBodyNode.get("tool_calls").size() > 0) {
                        JsonNode toolCall = firstBodyNode.get("tool_calls").get(0);
                        // Convert to Map<String, Object>
                        Map<String, Object> toolCallMap = objectMapper.convertValue(toolCall, Map.class);
                        messages.add(new ChatMessage(toolCallMap));
                    }

                    // Create new prompt with updated messages
                    FormattedPrompt<List<ChatMessage>> newPrompt = 
                            templatePrompt.bind(new TemplatePrompt.BindRequest(testCase.getVariables()).history(messages)).format();
                    
                    return callAnthropic(
                            objectMapper,
                            anthropicApiKey,
                            newPrompt.getPromptInfo().getModel(),
                            newPrompt.getPromptInfo().getModelParameters(),
                            newPrompt.getFormattedPrompt(),
                            newPrompt.getSystemContent().orElse(null),
                            newPrompt.getToolSchema()
                    ).thenCompose(secondResponse -> {
                        JsonNode secondBodyNode;
                        try {
                            secondBodyNode = objectMapper.readTree(secondResponse.body());
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Unable to parse second response body.", e);
                        }
                        
                        System.out.println("Second Completion: " + secondBodyNode.path("completion").asText());
                        
                        formattedPrompt.allMessages(
                            new ChatMessage("assistant", secondBodyNode.path("completion").asText())
                        );

                        return recordAnthropic(fpClient, projectId, testRun, session, testCase, formattedPrompt, startTime, secondResponse);
                    });
                })
        );
    }

    private static CompletableFuture<RecordResponse> recordAnthropic(
            Freeplay fpClient,
            String projectId,
            TestRun testRun,
            Session session,
            CompletionTestCase testCase,
            FormattedPrompt<List<ChatMessage>> formattedPrompt,
            long startTime,
            HttpResponse<String> response
    ) {
        JsonNode bodyNode;
        try {
            bodyNode = objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse response body.", e);
        }

        List<Object> content = objectMapper.convertValue(bodyNode.get("content"), List.class);
        List<ChatMessage> allMessages = formattedPrompt.allMessages(
                new ChatMessage("assistant", content)
        );

        CallInfo callInfo = CallInfo.from(
                formattedPrompt.getPromptInfo(),
                startTime,
                System.currentTimeMillis()
        );
        ResponseInfo responseInfo = new ResponseInfo(
                "stop_sequence".equals(bodyNode.path("stop_reason").asText())
        );

        System.out.println("Completion: " + bodyNode.path("completion").asText());

        return fpClient.recordings().create(
                new RecordInfo(
                        projectId,
                        allMessages
                ).inputs(testCase.getVariables())
                        .promptVersionInfo(formattedPrompt.getPromptInfo())
                        .callInfo(callInfo)
                        .responseInfo(responseInfo)
                        .toolSchema(formattedPrompt.getToolSchema())
                        .testRunInfo(testRun.getTestRunInfo(testCase.getTestCaseId())));
    }
}
