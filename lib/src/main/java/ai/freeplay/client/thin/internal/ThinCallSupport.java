package ai.freeplay.client.thin.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.TemplateResolver;
import ai.freeplay.client.thin.internal.dto.RecordDTO;
import ai.freeplay.client.thin.internal.dto.TestListDTO;
import ai.freeplay.client.thin.internal.dto.TestRunDTO;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.resources.feedback.CustomerFeedbackResponse;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.testruns.TestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static ai.freeplay.client.internal.ParameterUtils.validateBasicMap;
import static ai.freeplay.client.internal.PromptUtils.getFinalEnvironment;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


public class ThinCallSupport {
    private final HttpConfig httpConfig;
    private final TemplateResolver templateResolver;
    private final String baseUrl;
    private final String freeplayApiKey;

    public ThinCallSupport(
            HttpConfig httpConfig,
            TemplateResolver templateResolver,
            String baseUrl,
            String freeplayApiKey
    ) {
        this.httpConfig = httpConfig;
        this.templateResolver = templateResolver;
        this.baseUrl = baseUrl;
        this.freeplayApiKey = freeplayApiKey;
    }

    public static String getActiveFlavorName(String callFlavorName, String templateFlavorName) {
        return callFlavorName != null ? callFlavorName : templateFlavorName;
    }

    public CompletableFuture<TemplateDTO> getPrompt(
            String projectId,
            String templateName,
            String environment
    ) {
        return templateResolver.getPrompt(projectId, templateName, getFinalEnvironment(environment));
    }

    public CompletableFuture<RecordResponse> record(RecordInfo recordPayload) {

        if (recordPayload.getAllMessages().isEmpty()) {
            throw new FreeplayClientException("Messages list must have at least one message. " +
                    "The last message should be the current response.");
        }
        String historyAsString = historyAsString(recordPayload.getAllMessages());
        ChatMessage completion = recordPayload.getAllMessages().get(recordPayload.getAllMessages().size() - 1);

        String testRunId =
                recordPayload.getTestRunInfo() == null
                        ? null
                        : recordPayload.getTestRunInfo().getTestRunId();
        String testCaseId =
                recordPayload.getTestRunInfo() == null
                        ? null
                        : recordPayload.getTestRunInfo().getTestCaseId();

        RecordDTO payload = new RecordDTO(
                recordPayload.getSessionInfo().getSessionId(),
                recordPayload.getPromptInfo().getPromptTemplateVersionId(),
                recordPayload.getPromptInfo().getPromptTemplateId(),
                recordPayload.getCallInfo().getStartTime(),
                recordPayload.getCallInfo().getEndTime(),
                recordPayload.getPromptInfo().getEnvironment(),
                recordPayload.getInputs(),
                recordPayload.getSessionInfo().getCustomMetadata(),
                historyAsString,
                completion.getContent(),
                recordPayload.getResponseInfo().isComplete(),
                testRunId,
                testCaseId,
                recordPayload.getCallInfo().getProvider(),
                recordPayload.getCallInfo().getModel(),
                recordPayload.getCallInfo().getModelParameters(),
                recordPayload.getResponseInfo().getFunctionCallMap()
        );
        return AsyncHttp.postJson(
                format("%s/v1/record", baseUrl),
                freeplayApiKey,
                httpConfig,
                payload
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);
            JsonNode responseNode = JSONUtil.parseDOM(httpResponse.body());
            return new RecordResponse(responseNode.path("completion_id").asText(null));
        });
    }

    public CompletableFuture<TestRun> createTestRun(String projectId, String testList, boolean includeOutputs) {
        String url = String.format("%s/projects/%s/test-runs-cases", baseUrl, projectId);
        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                new TestListDTO(testList, includeOutputs)
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);

            TestRunDTO testRun =
                    JSONUtil.parse(
                            httpResponse.body(),
                            TestRunDTO.class);

            return new TestRun(
                    testRun.getTestRunId(),
                    testRun.getTestCases().stream()
                            .map(testCase -> new TestCase(
                                    testCase.getId(),
                                    testCase.getVariables(),
                                    testCase.getOutput()
                            ))
                            .collect(toList())
            );
        });
    }

    public CompletableFuture<CustomerFeedbackResponse> updateCustomerFeedback(
            String completionId,
            Map<String, Object> feedback
    ) {
        validateBasicMap(feedback);
        String url = String.format("%s/v1/completion_feedback/%s", baseUrl, completionId);
        return AsyncHttp.putJson(
                url,
                freeplayApiKey,
                httpConfig,
                feedback
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);

            return new CustomerFeedbackResponse();
        });
    }

    private static String historyAsString(List<ChatMessage> allMessages) {
        List<ChatMessage> allButLast = allMessages.subList(0, allMessages.size() - 1);
        return JSONUtil.toString(allButLast);
    }
}
