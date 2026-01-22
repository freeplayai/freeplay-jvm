package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.TemplatePrompt;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.testruns.CompletionTestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.callAnthropic;
import static java.util.stream.Collectors.toList;

public class ThinTestRunHistoryExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        //noinspection unused
        String openAIApiKey = System.getenv("OPENAI_API_KEY");
        //noinspection unused
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .customerDomain(customerDomain)
        );
        String testRunName = "Test run: " + UUID.randomUUID();
        List<RecordResponse> recordResponses = fpClient.testRuns().create(projectId, "history-dataset", true, testRunName, "Run from JVM examples")
                .thenCompose(testRun ->
                        fpClient.prompts().get(projectId, "History-QA", "dev")
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
        System.out.println("Test Case History");
        System.out.println(testCase.getHistory());
        FormattedPrompt<List<ChatMessage>> formattedPrompt =
                templatePrompt.bind(new TemplatePrompt.BindRequest(testCase.getVariables()).history(testCase.getHistory())).format();

        long startTime = System.currentTimeMillis();
        return callAnthropic(
                objectMapper,
                anthropicApiKey,
                formattedPrompt.getPromptInfo().getModel(),
                formattedPrompt.getPromptInfo().getModelParameters(),
                formattedPrompt.getFormattedPrompt(),
                formattedPrompt.getSystemContent().orElse(null)
        ).thenCompose((HttpResponse<String> response) ->
                recordAnthropic(fpClient, projectId, testRun, testCase, formattedPrompt, startTime, response)
        );
    }

    private static CompletableFuture<RecordResponse> recordAnthropic(
            Freeplay fpClient,
            String projectId,
            TestRun testRun,
            CompletionTestCase testCase,
            FormattedPrompt<List<ChatMessage>> formattedPrompt,
            long startTime,
            HttpResponse<String> response
    ) {
        JsonNode bodyNode;
        System.out.println("Response: " + response.body());
        try {
            bodyNode = objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse response body.", e);
        }

        List<ChatMessage> allMessages = formattedPrompt.allMessages(
                new ChatMessage("Assistant", bodyNode.path("content").get(0).path("text").asText())
        );
        System.out.println("All messages: " + allMessages);
        CallInfo callInfo = CallInfo.from(
                formattedPrompt.getPromptInfo(),
                startTime,
                System.currentTimeMillis()
        );
        ResponseInfo responseInfo = new ResponseInfo(
                "stop_sequence".equals(bodyNode.path("stop_reason").asText())
        );

        System.out.println("Completion: " + bodyNode.path("content").get(0).path("text").asText());

        return fpClient.recordings().create(
                new RecordInfo(
                        projectId,
                        allMessages
                ).inputs(testCase.getVariables())
                        .promptVersionInfo(formattedPrompt.getPromptInfo())
                        .callInfo(callInfo)
                        .responseInfo(responseInfo)
                        .testRunInfo(testRun.getTestRunInfo(testCase.getTestCaseId())));
    }
}
