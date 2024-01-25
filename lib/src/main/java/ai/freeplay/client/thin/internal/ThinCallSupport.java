package ai.freeplay.client.thin.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.TemplateResolver;
import ai.freeplay.client.thin.internal.model.RecordAPIPayload;
import ai.freeplay.client.thin.internal.model.Template;
import ai.freeplay.client.thin.internal.model.Templates;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static ai.freeplay.client.internal.PromptUtils.getFinalTag;
import static java.lang.String.format;


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

    public CompletableFuture<Template> getPrompt(
            String projectId,
            String templateName,
            String environment
    ) {
        return getPrompts(projectId, environment)
                .thenApply((Templates templates) ->
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

        RecordAPIPayload payload = new RecordAPIPayload(
                recordPayload.getSessionId(),
                recordPayload.getPromptInfo().getPromptTemplateVersionId(),
                recordPayload.getPromptInfo().getPromptTemplateId(),
                recordPayload.getCallInfo().getStartTime(),
                recordPayload.getCallInfo().getEndTime(),
                recordPayload.getPromptInfo().getEnvironment(),
                recordPayload.getInputs(),
                recordPayload.getCallInfo().getCustomMetadata(),
                historyAsString,
                completion.getContent(),
                recordPayload.getResponseInfo().isComplete(),
                testRunId,
                recordPayload.getCallInfo().getProvider(),
                recordPayload.getCallInfo().getModel(),
                recordPayload.getCallInfo().getModelParameters()
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

    private static String historyAsString(List<ChatMessage> allMessages) {
        List<ChatMessage> allButLast = allMessages.subList(0, allMessages.size() - 1);
        return JSONUtil.toString(allButLast);
    }

    private CompletableFuture<Templates> getPrompts(String projectId, String tag) {
        String finalTag = getFinalTag(tag);
        return templateResolver.getPrompts(projectId, finalTag);
    }

    private Optional<Template> findPrompt(Templates templates, String templateName) {
        return templates
                .getTemplates()
                .stream()
                .filter(template -> template.getName().equals(templateName))
                .findFirst();
    }
}
