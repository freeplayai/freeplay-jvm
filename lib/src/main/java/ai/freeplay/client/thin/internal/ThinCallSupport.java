package ai.freeplay.client.thin.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.TemplateResolver;
import ai.freeplay.client.thin.internal.dto.*;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.testruns.TestCase;
import ai.freeplay.client.thin.resources.testruns.TestRun;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static ai.freeplay.client.internal.PromptUtils.getFinalTag;
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
        return getPrompts(projectId, environment)
                .thenApply((TemplatesDTO templates) ->
                        findPrompt(templates, templateName).orElseThrow(
                                () -> new FreeplayConfigurationException(
                                        format("Could not find template %s in environment %s.%n", templateName, environment)
                                )
                        )
                );
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

    public CompletableFuture<TestRun> createTestRun(String projectId, String testList) {
        String url = String.format("%s/projects/%s/test-runs-cases", baseUrl, projectId);
        return AsyncHttp.postJson(
                url,
                freeplayApiKey,
                httpConfig,
                new TestListDTO(testList)
        ).thenApply(httpResponse -> {
            throwFreeplayIfError(httpResponse, 201);

            TestRunDTO testRun =
                    JSONUtil.parse(
                            httpResponse.body(),
                            TestRunDTO.class);

            return new TestRun(
                    testRun.getTestRunId(),
                    testRun.getTestCases().stream()
                            .map(testCase -> new TestCase(testCase.getId(), testCase.getVariables()))
                            .collect(toList())
            );
        });
    }

    private static String historyAsString(List<ChatMessage> allMessages) {
        List<ChatMessage> allButLast = allMessages.subList(0, allMessages.size() - 1);
        return JSONUtil.toString(allButLast);
    }

    private CompletableFuture<TemplatesDTO> getPrompts(String projectId, String tag) {
        String finalTag = getFinalTag(tag);
        return templateResolver.getPrompts(projectId, finalTag);
    }

    private Optional<TemplateDTO> findPrompt(TemplatesDTO templates, String templateName) {
        return templates
                .getTemplates()
                .stream()
                .filter(template -> template.getName().equals(templateName))
                .findFirst();
    }
}
