package ai.freeplay.client.internal;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.RecordProcessor;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.FreeplayServerException;
import ai.freeplay.client.flavor.*;
import ai.freeplay.client.model.*;
import ai.freeplay.client.processor.ChatPromptProcessor;
import ai.freeplay.client.processor.LLMCallInfo;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.authHeaders;
import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

public class CallSupport {
    // Using the Freeplay class to make it nicer, since these will be in customer application logs
    public static final System.Logger LOGGER = System.getLogger(Freeplay.class.getName());

    private final String freeplayApiKey;
    private final String baseUrl;
    private final ChatFlavor clientFlavor;
    private final Map<String, Object> clientLLMParameters;
    private final ProviderConfigs providerConfig;
    private final HttpConfig httpConfig;
    private final RecordProcessor recordProcessor;

    public CallSupport(
            String freeplayApiKey,
            String baseUrl,
            ProviderConfigs providerConfig,
            ChatFlavor flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig,
            RecordProcessor recordProcessor
    ) {
        this.freeplayApiKey = freeplayApiKey;
        this.baseUrl = baseUrl;
        this.providerConfig = providerConfig;
        this.clientFlavor = flavor;
        this.clientLLMParameters = llmParameters != null ? llmParameters : Collections.emptyMap();
        this.httpConfig = httpConfig;
        this.recordProcessor = recordProcessor != null ?
                recordProcessor :
                new DefaultRecordProcessor();
    }

    public String createSession(String projectId, String tag, Map<String, Object> metadata) throws FreeplayException {
        String finalTag = getFinalTag(tag);
        String url = getUrl("projects/%s/sessions/tag/%s", projectId, finalTag);

        validateBasicMap(metadata);

        HttpResponse<String> response;
        try {
            response = Http.postJsonWithBearer(url, Map.of("metadata", metadata), freeplayApiKey, httpConfig);
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error creating session.", e);
        }
        throwFreeplayIfError(response, 201);

        try {
            Map<String, Object> sessionMap = Http.parseBody(response);
            return valueOf(sessionMap.get("session_id"));
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error creating session.", e);
        }
    }

    private static void validateBasicMap(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!(entry.getValue() instanceof String || entry.getValue() instanceof Number || entry.getValue() instanceof Boolean)) {
                throw new FreeplayClientException("Invalid value for key '" + entry.getKey() +
                        "': Value must be a string or number.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<PromptTemplate> getPrompts(String projectId, String tag) throws FreeplayException {
        String finalTag = getFinalTag(tag);
        String url = getUrl("projects/%s/templates/all/%s", projectId, finalTag);
        HttpResponse<String> response = Http.get(url, freeplayApiKey, httpConfig);
        throwFreeplayIfError(response, 200);

        Map<String, Object> templatesMap;
        try {
            templatesMap = Http.parseBody(response);
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error getting prompts.", e);
        }
        List<Map<String, Object>> templates = (List<Map<String, Object>>) templatesMap.get("templates");

        return templates.stream().map((Object template) -> {
            Map<String, Object> templateMap = (Map<String, Object>) template;
            return new PromptTemplate(
                    valueOf(templateMap.get("name")),
                    valueOf(templateMap.get("content")),
                    valueOf(templateMap.get("flavor_name")),
                    valueOf(templateMap.get("project_version_id")),
                    valueOf(templateMap.get("prompt_template_id")),
                    valueOf(templateMap.get("prompt_template_version_id")),
                    (Map<String, Object>) templateMap.get("params"));
        }).collect(toList());
    }

    public Optional<PromptTemplate> findPrompt(Collection<PromptTemplate> templates, String templateName) {
        return templates.stream()
                .filter(template -> template.getName().equals(templateName))
                .findFirst();
    }

    public TestRun createTestRun(String projectId, String environment, String testListName) {
        String url = getUrl("projects/%s/test-runs", projectId);
        HttpResponse<String> response;
        try {
            response = Http.postJsonWithBearer(
                    url,
                    Map.of("playlist_name", testListName),
                    freeplayApiKey,
                    httpConfig
            );
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error creating test run.", e);
        }
        throwFreeplayIfError(response, 201);

        Map<String, Object> objectMap;
        try {
            objectMap = Http.parseBody(response);
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error creating test run.", e);
        }

        String testRunId = valueOf(objectMap.get("test_run_id"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inputs = (List<Map<String, Object>>) objectMap.get("inputs");
        return new TestRun(this, projectId, environment, testRunId, inputs);
    }

    public CompletionResponse prepareAndMakeCall(
            String sessionId,
            Collection<PromptTemplate> templates,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            ChatFlavor flavor,
            ChatPromptProcessor promptProcessor
    ) throws FreeplayException {
        Optional<PromptTemplate> maybePrompt = findPrompt(templates, templateName);
        if (maybePrompt.isEmpty()) {
            throw new FreeplayConfigurationException(
                    "Prompt template " + templateName + " in environment " + tag + " not found.");
        }
        PromptTemplate template = maybePrompt.get();

        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        ChatFlavor activeFlavor = getActiveFlavor(flavor, template);

        Collection<ChatMessage> formattedPrompt = activeFlavor.formatPrompt(template.getContent(), variables);
        Collection<ChatMessage> modifiedPrompt = promptProcessor != null ?
                promptProcessor.apply(
                        formattedPrompt,
                        new LLMCallInfo(activeFlavor.getProviderEnum(), mergedLLMParameters)) :
                formattedPrompt;

        Instant start = Instant.ofEpochMilli(currentTimeMillis());
        CompletionResponse response = activeFlavor.callService(
                modifiedPrompt,
                providerConfig,
                mergedLLMParameters,
                httpConfig);
        Instant end = Instant.ofEpochMilli(currentTimeMillis());
        Optional<String> completionId = recordProcessor.record(
                new PromptInfo(
                        template.getPromptTemplateVersionId(),
                        template.getPromptTemplateId(),
                        activeFlavor.getFormatType(),
                        activeFlavor.getProvider(),
                        valueOf(mergedLLMParameters.get("model")),
                        mergedLLMParameters
                ),
                new CallInfo(
                        sessionId,
                        testRunId,
                        start,
                        end,
                        tag,
                        variables,
                        activeFlavor.serializeForRecord(modifiedPrompt),
                        response.getContent(),
                        response.isComplete()
                )
        );
        completionId.ifPresent(response::setCompletionId);

        return response;
    }

    public Stream<IndexedChatMessage> makeCallStream(
            String sessionId,
            PromptTemplate template,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            ChatFlavor callFlavor,
            ChatPromptProcessor promptProcessor
    ) throws FreeplayException {
        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        ChatFlavor activeFlavor = getActiveFlavor(callFlavor, template);

        Collection<ChatMessage> formattedPrompt = activeFlavor.formatPrompt(template.getContent(), variables);
        Collection<ChatMessage> modifiedPrompt = promptProcessor != null ?
                promptProcessor.apply(
                        formattedPrompt,
                        new LLMCallInfo(activeFlavor.getProviderEnum(), mergedLLMParameters)) :
                formattedPrompt;

        Instant start = Instant.ofEpochMilli(currentTimeMillis());
        Stream<IndexedChatMessage> responseStream = activeFlavor.callServiceStream(
                modifiedPrompt,
                providerConfig,
                mergedLLMParameters,
                httpConfig);

        return handleStream(
                sessionId,
                template,
                variables,
                tag,
                testRunId,
                mergedLLMParameters,
                activeFlavor,
                modifiedPrompt,
                start,
                responseStream);
    }

    public ChatCompletionResponse makeContinueChatCall(
            String sessionId,
            Collection<PromptTemplate> templates,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            ChatFlavor flavor,
            ChatPromptProcessor promptProcessor
    ) throws FreeplayException {
        Optional<PromptTemplate> maybePrompt = findPrompt(templates, templateName);
        return maybePrompt.map((PromptTemplate prompt) -> {
            ChatFlavor activeFlavor = getActiveFlavor(flavor, prompt);
            Collection<ChatMessage> formattedPrompt = activeFlavor.formatPrompt(prompt.getContent(), variables);
            return makeContinueChatCall(
                    sessionId,
                    prompt,
                    formattedPrompt,
                    variables,
                    llmParameters,
                    tag,
                    testRunId,
                    promptProcessor
            );
        }).orElseThrow(() ->
                new FreeplayConfigurationException(format("Prompt template %s not found in environment %s.", templateName, tag))
        );
    }

    public ChatCompletionResponse makeContinueChatCall(
            String sessionId,
            PromptTemplate template,
            Collection<ChatMessage> formattedMessages,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            ChatPromptProcessor promptProcessor
    ) throws FreeplayException {
        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        ChatFlavor activeFlavor = getActiveFlavor(clientFlavor, template);

        Collection<ChatMessage> finalMessages = promptProcessor != null ?
                promptProcessor.apply(
                        formattedMessages,
                        new LLMCallInfo(activeFlavor.getProviderEnum(), mergedLLMParameters)) :
                formattedMessages;

        Instant start = Instant.ofEpochMilli(currentTimeMillis());
        ChatCompletionResponse response = activeFlavor.callChatService(
                finalMessages, providerConfig, mergedLLMParameters, httpConfig);
        Instant end = Instant.ofEpochMilli(currentTimeMillis());

        Optional<String> completionId = recordProcessor.record(
                new PromptInfo(
                        template.getPromptTemplateVersionId(),
                        template.getPromptTemplateId(),
                        activeFlavor.getFormatType(),
                        activeFlavor.getProvider(),
                        valueOf(mergedLLMParameters.get("model")),
                        mergedLLMParameters
                ),
                new CallInfo(
                        sessionId,
                        testRunId,
                        start,
                        end,
                        tag,
                        variables,
                        activeFlavor.serializeForRecord(finalMessages),
                        response.getContent(),
                        response.isComplete()
                )
        );
        completionId.ifPresent(response::setCompletionId);
        return response;
    }

    public Stream<IndexedChatMessage> makeContinueChatCallStream(
            String sessionId,
            PromptTemplate template,
            Collection<ChatMessage> formattedMessages,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId
    ) throws FreeplayException {
        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        ChatFlavor activeFlavor = getActiveFlavor(clientFlavor, template);

        Instant start = Instant.ofEpochMilli(currentTimeMillis());
        Stream<IndexedChatMessage> responseStream = activeFlavor.callServiceStream(
                formattedMessages, providerConfig, mergedLLMParameters, httpConfig);

        return handleStream(
                sessionId,
                template,
                variables,
                tag,
                testRunId,
                mergedLLMParameters,
                activeFlavor,
                formattedMessages,
                start,
                responseStream);
    }

    private Stream<IndexedChatMessage> handleStream(
            String sessionId,
            PromptTemplate template,
            Map<String, Object> variables,
            String tag,
            String testRunId,
            Map<String, Object> mergedLLMParameters,
            ChatFlavor activeFlavor,
            Collection<ChatMessage> formattedPrompt,
            Instant start,
            Stream<IndexedChatMessage> responseStream
    ) {
        AtomicReference<String> aggregatedContent = new AtomicReference<>("");
        return responseStream.
                map((IndexedChatMessage chunk) -> {
                    aggregatedContent.getAndUpdate((String previous) -> previous + activeFlavor.getContentFromChunk(chunk));
                    if (activeFlavor.isLastChunk(chunk)) {
                        Instant end = Instant.ofEpochMilli(currentTimeMillis());
                        Optional<String> completionId = recordProcessor.record(
                                new PromptInfo(
                                        template.getPromptTemplateVersionId(),
                                        template.getPromptTemplateId(),
                                        activeFlavor.getFormatType(),
                                        activeFlavor.getProvider(),
                                        valueOf(mergedLLMParameters.get("model")),
                                        mergedLLMParameters
                                ),
                                new CallInfo(
                                        sessionId,
                                        testRunId,
                                        start,
                                        end,
                                        tag,
                                        variables,
                                        activeFlavor.serializeForRecord(formattedPrompt),
                                        aggregatedContent.get(),
                                        activeFlavor.isComplete(chunk)
                                )
                        );
                        completionId.ifPresent(chunk::setCompletionId);
                    }
                    return chunk;
                });
    }

    public ChatFlavor getActiveFlavor(ChatFlavor callFlavor, PromptTemplate prompt) {
        if (callFlavor != null) return callFlavor;
        if (this.clientFlavor != null) return this.clientFlavor;

        String flavorName = prompt.getFlavorName();
        switch (flavorName) {
            case "openai_chat":
                return new OpenAIChatFlavor();
            case "anthropic_chat":
                return new AnthropicChatFlavor();
            default:
                throw new FreeplayConfigurationException(format("Unable to create Flavor for name '%s'.%n", flavorName));
        }
    }

    private Map<String, Object> getMergedParameters(
            PromptTemplate promptTemplate,
            Map<String, Object> callLLMParameters
    ) {
        Map<String, Object> merged = new HashMap<>(16);
        merged.putAll(promptTemplate.getLLMParameters());
        merged.putAll(clientLLMParameters);
        merged.putAll(callLLMParameters);
        return merged;
    }

    private static String getFinalTag(String tag) {
        return tag != null ? tag : "latest";
    }

    private String getUrl(String path, Object... args) {
        return format("%s/%s", baseUrl, format(path, args));
    }

    private class DefaultRecordProcessor implements RecordProcessor {
        @Override
        public Optional<String> record(PromptInfo promptInfo, CallInfo callInfo) {
            String url = getUrl("v1/record");
            Map<String, Object> payload = new HashMap<>(32);
            payload.put("session_id", callInfo.getSessionId());
            payload.put("project_version_id", promptInfo.getPromptTemplateVersionId());
            payload.put("prompt_template_id", promptInfo.getPromptTemplateId());
            payload.put("start_time", callInfo.getStartTime());
            payload.put("end_time", callInfo.getEndTime());
            payload.put("tag", callInfo.getTag());
            payload.put("inputs", callInfo.getInputs());
            payload.put("prompt_content", callInfo.getPromptContent());
            payload.put("return_content", callInfo.getReturnContent());
            payload.put("format_type", promptInfo.getFormatType());
            payload.put("is_complete", callInfo.isComplete());
            payload.put("test_run_id", callInfo.getTestRunId());
            payload.put("provider", promptInfo.getProvider());
            payload.put("model", promptInfo.getModel());
            payload.put("llm_parameters", promptInfo.getLLMParameters());

            try {
                HttpResponse<String> response = Http.postJsonWithBearer(url, payload, freeplayApiKey);
                Map<String, Object> objectMap = Http.parseBody(response);
                String completionId = valueOf(objectMap.get("completion_id"));
                return Optional.of(completionId);
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Unable to record LLM call. Cause: {0}", e.getMessage());
                return Optional.empty();
            }
        }
    }

    public void recordCompletionFeedback(String completionId, Map<String, Object> feedback) throws FreeplayException {
        validateBasicMap(feedback);

        String url = getUrl("v1/completion_feedback/%s", completionId);
        try {
            Http.jsonRequest(
                    url,
                    JSONUtil.asString(feedback),
                    HttpResponse.BodyHandlers.ofString(),
                    httpConfig,
                    "PUT",
                    authHeaders(freeplayApiKey));
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error creating session.", e);
        }
    }
}
