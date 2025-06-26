package ai.freeplay.client.internal;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.RecordProcessor;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.FreeplayServerException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.flavor.Flavors;
import ai.freeplay.client.model.*;
import ai.freeplay.client.processor.ChatPromptProcessor;
import ai.freeplay.client.processor.LLMCallInfo;
import ai.freeplay.client.processor.TemplateResolver;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.authHeaders;
import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static ai.freeplay.client.internal.PromptUtils.getFinalEnvironment;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

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
    private final TemplateResolver templateResolver;

    public CallSupport(
            String freeplayApiKey,
            String baseUrl,
            ProviderConfigs providerConfig,
            ChatFlavor flavor,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig,
            RecordProcessor recordProcessor,
            TemplateResolver templateResolver
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
        this.templateResolver = templateResolver;
    }

    public static String createSessionId() throws FreeplayException {
        return randomUUID().toString();
    }

    public Collection<PromptTemplate> getPrompts(String projectId, String tag) throws FreeplayException {
        String finalEnvironment = getFinalEnvironment(tag);
        return templateResolver.getPrompts(projectId, finalEnvironment);
    }

    public Optional<PromptTemplate> findPrompt(Collection<PromptTemplate> templates, String templateName) {
        return templates.stream()
                .filter(template -> template.getName().equals(templateName))
                .findFirst();
    }

    public TestRun createTestRun(String projectId, String environment, String testListName) {
        String url = getUrl("v2/projects/%s/test-runs", projectId);
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
            Map<String, Object> customMetadata,
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
                        customMetadata,
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
            Map<String, Object> customMetadata,
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
                customMetadata,
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
            Map<String, Object> customMetadata,
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
                    customMetadata,
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
            Map<String, Object> customMetadata,
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
                        customMetadata,
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
            Map<String, Object> customMetadata,
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
                customMetadata,
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
            Map<String, Object> customMetadata,
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
                                        customMetadata,
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
        return Flavors.getFlavorByName(flavorName);
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
            payload.put("custom_metadata", callInfo.getCustomMetadata());
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

    public void recordCompletionFeedback(String projectId, String completionId, Map<String, Object> feedback) throws FreeplayException {
        ParameterUtils.validateBasicMap(feedback);

        String url = getUrl("v2/projects/%s/completion-feedback/id/%s", projectId, completionId);
        try {
            Http.jsonRequest(
                    url,
                    JSONUtil.asString(feedback),
                    HttpResponse.BodyHandlers.ofString(),
                    httpConfig,
                    "POST",
                    authHeaders(freeplayApiKey));
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error creating session.", e);
        }
    }
}
