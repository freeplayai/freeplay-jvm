package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.TemplatePrompt;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.sessions.SessionInfo;
import ai.freeplay.client.thin.resources.testruns.TestCase;
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

public class ThinTestRunExample {
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
        List<RecordResponse> recordResponses = fpClient.testRuns().create(projectId, "20-q", false, testRunName, "Run from JVM examples")
                .thenCompose(testRun ->
                        fpClient.prompts().get(projectId, "my-prompt-anthropic", "prod")
                                .thenCompose(templatePrompt -> {

                                    var futures =
                                            testRun.getTestCases().stream()
                                                    .map(testCase ->
                                                            handleTestCase(
                                                                    fpClient,
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
            String anthropicApiKey,
            TemplatePrompt templatePrompt,
            TestRun testRun,
            TestCase testCase
    ) {
        FormattedPrompt<List<ChatMessage>> formattedPrompt =
                templatePrompt.bind(testCase.getVariables()).format();

        long startTime = System.currentTimeMillis();
        return callAnthropic(
                objectMapper,
                anthropicApiKey,
                formattedPrompt.getPromptInfo().getModel(),
                formattedPrompt.getPromptInfo().getModelParameters(),
                formattedPrompt.getFormattedPrompt(),
                formattedPrompt.getSystemContent().orElse(null)
        ).thenCompose((HttpResponse<String> response) ->
                recordAnthropic(fpClient, testRun, testCase, formattedPrompt, startTime, response)
        );
    }

    private static CompletableFuture<RecordResponse> recordAnthropic(
            Freeplay fpClient,
            TestRun testRun,
            TestCase testCase,
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

        List<ChatMessage> allMessages = formattedPrompt.allMessages(
                new ChatMessage("Assistant", bodyNode.path("completion").asText())
        );
        CallInfo callInfo = CallInfo.from(
                formattedPrompt.getPromptInfo(),
                startTime,
                System.currentTimeMillis()
        );
        ResponseInfo responseInfo = new ResponseInfo(
                "stop_sequence".equals(bodyNode.path("stop_reason").asText())
        );
        SessionInfo sessionInfo = fpClient.sessions().create().getSessionInfo();

        System.out.println("Completion: " + bodyNode.path("completion").asText());

        return fpClient.recordings().create(
                new RecordInfo(
                        allMessages,
                        testCase.getVariables(),
                        sessionInfo,
                        formattedPrompt.getPromptInfo(),
                        callInfo,
                        responseInfo
                ).testRunInfo(testRun.getTestRunInfo(testCase.getTestCaseId())));
    }
}
