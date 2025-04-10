package ai.freeplay.client.thin.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.TemplateResolver;
import ai.freeplay.client.thin.internal.dto.*;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.resources.feedback.CustomerFeedbackResponse;
import ai.freeplay.client.thin.resources.feedback.TraceFeedbackResponse;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.sessions.SessionDeleteResponse;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;
import ai.freeplay.client.thin.resources.sessions.TraceRecordResponse;
import ai.freeplay.client.thin.resources.testruns.TestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import ai.freeplay.client.thin.resources.testruns.TestRunResults;
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

    public CompletableFuture<TemplateDTO> getPromptByVersionId(
            String projectId,
            String templateId,
            String templateVersionId
    ) {
        return templateResolver.getPromptByVersionId(projectId, templateId, templateVersionId);
    }

    public CompletableFuture<RecordResponse> record(RecordInfo recordPayload) {

        if (recordPayload.getAllMessages().isEmpty()) {
            throw new FreeplayClientException("Messages list must have at least one message. " +
                    "The last message should be the current response.");
        }

        String testRunId =
                recordPayload.getTestRunInfo() == null
                        ? null
                        : recordPayload.getTestRunInfo().getTestRunId();
        String testCaseId =
                recordPayload.getTestRunInfo() == null
                        ? null
                        : recordPayload.getTestRunInfo().getTestCaseId();

        RecordDTO.ResponseInfoDTO responseInfo = new RecordDTO.ResponseInfoDTO(
                recordPayload.getResponseInfo().isComplete(),
                recordPayload.getResponseInfo().getFunctionCall() != null ? new RecordDTO.OpenAIFunctionCallDTO(
                        recordPayload.getResponseInfo().getFunctionCall().getName(),
                        recordPayload.getResponseInfo().getFunctionCall().getArguments()
                ) : null,
                recordPayload.getResponseInfo().getPromptTokens(),
                recordPayload.getResponseInfo().getResponseTokens()
        );

        RecordDTO payload = new RecordDTO(
                recordPayload.getAllMessages(),
                recordPayload.getInputs(),
                new RecordDTO.SessionInfoDTO(recordPayload.getSessionInfo().getSessionId(), recordPayload.getSessionInfo().getCustomMetadata()),
                new RecordDTO.PromptInfoDTO(recordPayload.getPromptInfo().getPromptTemplateId(), recordPayload.getPromptInfo().getPromptTemplateVersionId(),
                        recordPayload.getPromptInfo().getTemplateName(), recordPayload.getPromptInfo().getEnvironment(),
                        recordPayload.getPromptInfo().getModelParameters(), recordPayload.getPromptInfo().getProviderInfo(),
                        recordPayload.getPromptInfo().getProvider(), recordPayload.getPromptInfo().getModel(),
                        recordPayload.getPromptInfo().getFlavorName(), recordPayload.getPromptInfo().getProjectId()),
                new RecordDTO.CallInfoDTO(recordPayload.getCallInfo().getProvider(), recordPayload.getCallInfo().getModel(),
                        recordPayload.getCallInfo().getStartTime(), recordPayload.getCallInfo().getEndTime(), recordPayload.getCallInfo().getModelParameters()
                ).providerInfo(
                        recordPayload.getCallInfo().getProviderInfo()
                ).usage(
                        recordPayload.getCallInfo().getUsage() != null ?
                                new RecordDTO.CallInfoDTO.UsageTokensDTO(
                                        recordPayload.getCallInfo().getUsage().getPromptTokens(),
                                        recordPayload.getCallInfo().getUsage().getCompletionTokens()
                                ) : null
                ).apiStyle(recordPayload.getCallInfo().getApiStyle()),
                responseInfo,
                testRunId != null ? new RecordDTO.TestRunInfoDTO(testRunId, testCaseId) : null,
                recordPayload.getEvalResults(),
                recordPayload.getTraceInfo() != null ? new RecordDTO.TraceInfoDTO(recordPayload.getTraceInfo().traceId) : null,
                recordPayload.getToolSchema(),
                recordPayload.getCompletionId()
        );

        return AsyncHttp.postJson(
                format("%s/v2/projects/%s/sessions/%s/completions", baseUrl, recordPayload.getPromptInfo().getProjectId(), recordPayload.getSessionInfo().getSessionId()),
                freeplayApiKey,
                httpConfig,
                payload
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);
            JsonNode responseNode = JSONUtil.parseDOM(httpResponse.body());
            return new RecordResponse(responseNode.path("completion_id").asText(null));
        });
    }

    @SuppressWarnings("unused")
    public CompletableFuture<TraceRecordResponse> recordTrace(String projectId, TraceInfo traceInfo) {
        if (traceInfo.input.isEmpty()) {
            throw new FreeplayClientException("Input needed to record a trace");
        }
        TraceInfoDTO payload = new TraceInfoDTO(
                traceInfo.input,
                traceInfo.output
        );
        return AsyncHttp.postJson(
                format("%s/v2/projects/%s/sessions/%s/traces/id/%s", baseUrl, projectId, traceInfo.sessionId, traceInfo.traceId),
                freeplayApiKey,
                httpConfig,
                payload
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);
            return new TraceRecordResponse();
        });
    }

    public CompletableFuture<TestRun> createTestRun(String projectId, String testList, boolean includeOutputs, String name, String description, String flavorName) {
        String url = String.format("%s/v2/projects/%s/test-runs", baseUrl, projectId);
        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                new TestListDTO(testList, includeOutputs, name, description, flavorName)
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
                                    testCase.getTestCaseId(),
                                    testCase.getVariables(),
                                    testCase.getOutput(),
                                    testCase.getHistory()
                            ))
                            .collect(toList())
            );
        });
    }

    public CompletableFuture<TestRunResults> getTestRunResults(String projectId, String testRunId) {
        String url = String.format("%s/v2/projects/%s/test-runs/id/%s", baseUrl, projectId, testRunId);
        return AsyncHttp.get(
                url,
                freeplayApiKey,
                httpConfig
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 200);

            TestRunResultsDTO testRunResults = JSONUtil.parse(httpResponse.body(), TestRunResultsDTO.class);
            return new TestRunResults(
                    testRunResults.getId(),
                    testRunResults.getName(),
                    testRunResults.getDescription(),
                    testRunResults.getSummaryStatistics()
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

    public CompletableFuture<TraceFeedbackResponse> updateTraceFeedback(
            String projectId,
            String traceId,
            Map<String, Object> feedback
    ) {
        validateBasicMap(feedback);
        String url = String.format("%s/v2/projects/%s/trace-feedback/id/%s", baseUrl, projectId, traceId);
        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                feedback
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);

            return new TraceFeedbackResponse();
        });
    }

    public CompletableFuture<SessionDeleteResponse> deleteSession(String projectId, String sessionId) {
        String url = String.format("%s/v2/projects/%s/sessions/%s", baseUrl, projectId, sessionId);
        return AsyncHttp.delete(
                url,
                freeplayApiKey,
                httpConfig
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);
            return new SessionDeleteResponse();
        });
    }

    @SuppressWarnings("unused")
    private static String historyAsString(List<ChatMessage> allMessages) {
        List<ChatMessage> allButLast = allMessages.subList(0, allMessages.size() - 1);
        return JSONUtil.toString(allButLast);
    }
}
