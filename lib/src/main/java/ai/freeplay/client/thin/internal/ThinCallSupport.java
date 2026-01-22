package ai.freeplay.client.thin.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.media.MediaInput;
import ai.freeplay.client.thin.TemplateResolver;
import ai.freeplay.client.thin.internal.dto.*;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.resources.feedback.CustomerFeedbackResponse;
import ai.freeplay.client.thin.resources.feedback.TraceFeedbackResponse;
import ai.freeplay.client.thin.resources.metadata.MetadataUpdateResponse;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.TemplateVersionResponse;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.TestRunInfo;
import ai.freeplay.client.thin.resources.sessions.SessionDeleteResponse;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;
import ai.freeplay.client.thin.resources.sessions.TraceRecordResponse;
import ai.freeplay.client.thin.resources.testruns.CompletionTestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import ai.freeplay.client.thin.resources.testruns.TestRunResults;
import ai.freeplay.client.thin.resources.testruns.TraceTestCase;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

        RecordDTO.ResponseInfoDTO responseInfo = null;
        if (recordPayload.getResponseInfo() != null) {
            responseInfo = new RecordDTO.ResponseInfoDTO(
                    recordPayload.getResponseInfo().isComplete(),
                    recordPayload.getResponseInfo().getFunctionCall() != null ? new RecordDTO.OpenAIFunctionCallDTO(
                            recordPayload.getResponseInfo().getFunctionCall().getName(),
                            recordPayload.getResponseInfo().getFunctionCall().getArguments()
                    ) : null,
                    recordPayload.getResponseInfo().getPromptTokens(),
                    recordPayload.getResponseInfo().getResponseTokens()
            );
        }

        Map<String, RecordDTO.MediaInputDTO> mediaInputs = new HashMap<>();
        if (recordPayload.getMediaInputCollection() != null) {
            for (Map.Entry<String, MediaInput> entry : recordPayload.getMediaInputCollection().entries()) {
                mediaInputs.put(entry.getKey(), RecordDTO.MediaInputDTO.fromMediaInput(entry.getValue()));
            }
        }

        // Handle optional PromptVersionInfo
        RecordDTO.PromptVersionInfoDTO promptVersionInfoDTO = null;
        if (recordPayload.getPromptVersionInfo() != null) {
            promptVersionInfoDTO = new RecordDTO.PromptVersionInfoDTO(
                    recordPayload.getPromptVersionInfo().getPromptTemplateVersionId(),
                    recordPayload.getPromptVersionInfo().getEnvironment(),
                    recordPayload.getProjectId()
            );
        }

        // Handle optional CallInfo
        RecordDTO.CallInfoDTO callInfoDTO = null;
        if (recordPayload.getCallInfo() != null) {
            callInfoDTO = new RecordDTO.CallInfoDTO(
                    recordPayload.getCallInfo().getProvider(),
                    recordPayload.getCallInfo().getModel(),
                    recordPayload.getCallInfo().getStartTime(),
                    recordPayload.getCallInfo().getEndTime(),
                    recordPayload.getCallInfo().getModelParameters()
            ).providerInfo(
                    recordPayload.getCallInfo().getProviderInfo()
            ).usage(
                    recordPayload.getCallInfo().getUsage() != null ?
                            new RecordDTO.CallInfoDTO.UsageTokensDTO(
                                    recordPayload.getCallInfo().getUsage().getPromptTokens(),
                                    recordPayload.getCallInfo().getUsage().getCompletionTokens()
                            ) : null
            ).apiStyle(recordPayload.getCallInfo().getApiStyle());
        }

        RecordDTO payload = new RecordDTO(
                recordPayload.getAllMessages(),
                recordPayload.getInputs(),
                new RecordDTO.SessionInfoDTO(recordPayload.getSessionInfo().getSessionId(), recordPayload.getSessionInfo().getCustomMetadata()),
                promptVersionInfoDTO,
                callInfoDTO,
                responseInfo,
                testRunId != null ? new RecordDTO.TestRunInfoDTO(testRunId, testCaseId) : null,
                recordPayload.getEvalResults(),
                recordPayload.getTraceInfo() != null ? new RecordDTO.TraceInfoDTO(recordPayload.getTraceInfo().traceId) : null,
                recordPayload.getParentId(),
                recordPayload.getToolSchema(),
                recordPayload.getOutputSchema(),
                recordPayload.getCompletionId(),
                mediaInputs
        );

        return AsyncHttp.postJson(
                format("%s/v2/projects/%s/sessions/%s/completions", baseUrl, recordPayload.getProjectId(), recordPayload.getSessionInfo().getSessionId()),
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
        return recordTrace(projectId, traceInfo, null);
    }

    public CompletableFuture<TraceRecordResponse> recordTrace(String projectId, TraceInfo traceInfo, TestRunInfo testRunInfo) {
        if (traceInfo.input == null) {
            throw new FreeplayClientException("Input needed to record a trace");
        }
        TraceInfoDTO payload = new TraceInfoDTO(
                traceInfo.getInput(),
                traceInfo.getOutput(),
                traceInfo.getAgentName(),
                traceInfo.getCustomMetadata(),
                traceInfo.getEvalResults(),
                testRunInfo,
                traceInfo.getParentId(),
                traceInfo.getKind(),
                traceInfo.getName(),
                traceInfo.getStartTime(),
                traceInfo.getEndTime()
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

    public CompletableFuture<TestRun> createTestRun(String projectId, String testList, boolean includeOutputs, String name, String description, String flavorName, List<UUID> targetEvaluationIds) {
        String url = String.format("%s/v2/projects/%s/test-runs", baseUrl, projectId);
        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                new TestListDTO(testList, includeOutputs, name, description, flavorName, targetEvaluationIds)
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);

            TestRunDTO testRun =
                    JSONUtil.parse(
                            httpResponse.body(),
                            TestRunDTO.class);

            List<CompletionTestCase> completionTestCases = null;
            List<TraceTestCase> traceTestCases = null;

            if (testRun.getTestCases() != null && !testRun.getTestCases().isEmpty()) {
                completionTestCases = testRun.getTestCases().stream()
                        .map(testCase -> new CompletionTestCase(
                                testCase.getTestCaseId(),
                                testCase.getVariables(),
                                testCase.getOutput(),
                                testCase.getHistory(),
                                testCase.getCustomMetadata()
                        ))
                        .collect(toList());
            }

            if (testRun.getTraceTestCases() != null && !testRun.getTraceTestCases().isEmpty()) {
                traceTestCases = testRun.getTraceTestCases().stream()
                        .map(testCase -> new TraceTestCase(
                                testCase.getTestCaseId(),
                                testCase.getInput(),
                                testCase.getOutput(),
                                testCase.getCustomMetadata()
                        ))
                        .collect(toList());
            }

            if (completionTestCases != null && !completionTestCases.isEmpty() &&
                    traceTestCases != null && !traceTestCases.isEmpty()) {
                throw new FreeplayClientException("Test cases and trace test cases cannot both be present.");
            }

            return new TestRun(testRun.getTestRunId(), completionTestCases, traceTestCases);
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
            String projectId,
            String completionId,
            Map<String, Object> feedback
    ) {
        validateBasicMap(feedback);
        String url = String.format("%s/v2/projects/%s/completion-feedback/id/%s", baseUrl, projectId, completionId);
        return AsyncHttp.postJson(
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

    public CompletableFuture<TemplateVersionResponse> createPromptVersion(
            String projectId,
            String promptTemplateName,
            List<TemplateDTO.Message> templateMessages,
            String model,
            String provider,
            String versionName,
            String versionDescription,
            Map<String, Object> llmParameters,
            List<TemplateDTO.ToolSchema> toolSchema,
            List<String> environments
    ) {
        String url = String.format(
                "%s/v2/projects/%s/prompt-templates/name/%s/versions",
                baseUrl,
                projectId,
                promptTemplateName
        );
        CreatePromptVersionDTO payload = new CreatePromptVersionDTO(
                promptTemplateName,
                templateMessages,
                model,
                provider,
                versionName,
                versionDescription,
                llmParameters,
                toolSchema,
                environments
        );

        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                payload
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);

            TemplateVersionResponseDTO responseDTO = JSONUtil.parse(
                    httpResponse.body(),
                    TemplateVersionResponseDTO.class
            );

            return new TemplateVersionResponse(
                    responseDTO.getPromptTemplateId(),
                    responseDTO.getPromptTemplateVersionId(),
                    responseDTO.getPromptTemplateName(),
                    responseDTO.getVersionName(),
                    responseDTO.getVersionDescription(),
                    new TemplateVersionResponse.PromptTemplateMetadata(
                            responseDTO.getMetadata().getProvider(),
                            responseDTO.getMetadata().getFlavor(),
                            responseDTO.getMetadata().getModel(),
                            responseDTO.getMetadata().getParams(),
                            responseDTO.getMetadata().getProviderInfo()
                    ),
                    responseDTO.getFormatVersion(),
                    responseDTO.getProjectId(),
                    responseDTO.getContent(),
                    responseDTO.getToolSchema()
            );
        });
    }

    public CompletableFuture<Void> updateTemplateVersionEnvironments(
            String projectId,
            String promptTemplateId,
            String promptTemplateVersionId,
            List<String> environments
    ) {
        String url = String.format(
                "%s/v2/projects/%s/prompt-templates/id/%s/versions/%s/environments",
                baseUrl,
                projectId,
                promptTemplateId,
                promptTemplateVersionId
        );
        UpdateVersionEnvironmentsDTO payload = new UpdateVersionEnvironmentsDTO(environments);

        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                payload
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 200);
            return null;
        });
    }

    public CompletableFuture<MetadataUpdateResponse> updateSessionMetadata(
            String projectId,
            String sessionId,
            Map<String, Object> metadata
    ) {
        validateBasicMap(metadata);
        String url = String.format(
                "%s/v2/projects/%s/sessions/id/%s/metadata",
                baseUrl,
                projectId,
                sessionId
        );
        return AsyncHttp.patchJson(
                url,
                freeplayApiKey,
                httpConfig,
                metadata
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 200);
            return new MetadataUpdateResponse();
        });
    }

    public CompletableFuture<MetadataUpdateResponse> updateTraceMetadata(
            String projectId,
            String sessionId,
            String traceId,
            Map<String, Object> metadata
    ) {
        validateBasicMap(metadata);
        String url = String.format(
                "%s/v2/projects/%s/sessions/%s/traces/id/%s/metadata",
                baseUrl,
                projectId,
                sessionId,
                traceId
        );
        return AsyncHttp.patchJson(
                url,
                freeplayApiKey,
                httpConfig,
                metadata
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 200);
            return new MetadataUpdateResponse();
        });
    }

    @SuppressWarnings("unused")
    private static String historyAsString(List<ChatMessage> allMessages) {
        List<ChatMessage> allButLast = allMessages.subList(0, allMessages.size() - 1);
        return JSONUtil.toString(allButLast);
    }
}
