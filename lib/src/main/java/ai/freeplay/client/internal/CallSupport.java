package ai.freeplay.client.internal;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.*;
import ai.freeplay.client.model.*;

import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class CallSupport {
    // Using the Freeplay class to make it nicer, since these will be in customer application logs
    public static final System.Logger LOGGER = System.getLogger(Freeplay.class.getName());

    private final String freeplayApiKey;
    private final String baseUrl;
    private final Flavor<?, ?> clientFlavor;
    private final Map<String, Object> clientLLMParameters;
    private final ProviderConfig providerConfig;

    public CallSupport(
            String freeplayApiKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Flavor<?, ?> flavor,
            Map<String, Object> llmParameters
    ) {
        this.freeplayApiKey = freeplayApiKey;
        this.baseUrl = baseUrl;
        this.providerConfig = providerConfig;
        this.clientFlavor = flavor;
        this.clientLLMParameters = llmParameters != null ? llmParameters : Collections.emptyMap();
    }

    public String createSession(String projectId, String tag) throws FreeplayException {
        String finalTag = getFinalTag(tag);
        String url = getUrl("projects/%s/sessions/tag/%s", projectId, finalTag);

        HttpResponse<String> response = Http.postWithBearer(url, freeplayApiKey);
        throwIfError(response, 201);

        Map<String, Object> sessionMap = Http.parseBody(response);
        return valueOf(sessionMap.get("session_id"));
    }

    @SuppressWarnings("unchecked")
    public Collection<PromptTemplate> getPrompts(String projectId, String tag) throws FreeplayException {
        String finalTag = getFinalTag(tag);
        String url = getUrl("projects/%s/templates/all/%s", projectId, finalTag);
        HttpResponse<String> response = Http.get(url, freeplayApiKey);
        throwIfError(response, 200);

        Map<String, Object> templatesMap = Http.parseBody(response);
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
        HttpResponse<String> response = Http.postJsonWithBearer(
                url,
                Map.of("playlist_name", testListName),
                freeplayApiKey
        );
        Map<String, Object> objectMap = Http.parseBody(response);

        String testRunId = valueOf(objectMap.get("test_run_id"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inputs = (List<Map<String, Object>>) objectMap.get("inputs");
        return new TestRun(this, projectId, environment, testRunId, inputs);
    }

    @SuppressWarnings("unchecked")
    public <P, R> CompletionResponse prepareAndMakeCall(
            String sessionId,
            Collection<PromptTemplate> templates,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            Flavor<P, R> flavor
    ) throws FreeplayException {
        Optional<PromptTemplate> maybePrompt = findPrompt(templates, templateName);
        if (maybePrompt.isEmpty()) {
            throw new FreeplayException(
                    "Prompt template " + templateName + " in environment " + tag + " not found.");
        }
        PromptTemplate template = maybePrompt.get();

        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        Flavor<P, R> activeFlavor = (Flavor<P, R>) getActiveFlavor(flavor, template);

        P formattedPrompt = activeFlavor.formatPrompt(template.getContent(), variables);

        double start = System.nanoTime() / 1e9;
        CompletionResponse response = activeFlavor.callService(formattedPrompt, providerConfig, mergedLLMParameters);
        double end = System.nanoTime() / 1e9;

        record(
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
                        response.getContent(),
                        response.isComplete()
                )
        );
        return response;
    }

    public ChatCompletionResponse makeContinueChatCall(
            String sessionId,
            PromptTemplate template,
            Collection<ChatMessage> formattedMessages,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId
    ) throws FreeplayException {
        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        ChatFlavor activeFlavor = getActiveChatFlavor(clientFlavor, template);

        double start = System.nanoTime() / 1e9;
        ChatCompletionResponse response = activeFlavor.callChatService(
                formattedMessages, providerConfig, mergedLLMParameters);
        double end = System.nanoTime() / 1e9;

        record(
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
                        activeFlavor.serializeForRecord(formattedMessages),
                        response.getContent(),
                        response.isComplete()
                )
        );
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
        ChatFlavor activeFlavor = getActiveChatFlavor(clientFlavor, template);

        double start = System.nanoTime() / 1e9;
        Stream<IndexedChatMessage> responseStream = activeFlavor.callServiceStream(
                formattedMessages, providerConfig, mergedLLMParameters);

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

    public <P, R> Stream<R> makeCallStream(
            String sessionId,
            PromptTemplate template,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            Flavor<P, R> callFlavor
    ) throws FreeplayException {
        Map<String, Object> mergedLLMParameters = getMergedParameters(template, llmParameters);
        @SuppressWarnings("unchecked")
        Flavor<P, R> activeFlavor = (Flavor<P, R>) getActiveFlavor(callFlavor, template);

        P formattedPrompt = activeFlavor.formatPrompt(template.getContent(), variables);

        double start = System.nanoTime() / 1e9;
        Stream<R> responseStream = activeFlavor.callServiceStream(formattedPrompt, providerConfig, mergedLLMParameters);

        return handleStream(
                sessionId,
                template,
                variables,
                tag,
                testRunId,
                mergedLLMParameters,
                activeFlavor,
                formattedPrompt,
                start,
                responseStream);
    }

    public ChatFlavor getActiveChatFlavor(Flavor<?, ?> flavor, PromptTemplate prompt) {
        Flavor<?, ?> activeFlavor = getActiveFlavor(flavor, prompt);

        if (!(activeFlavor instanceof ChatFlavor)) {
            throw new FreeplayException("Chat sessions must use an instance of ChatFlavor");
        }
        return (ChatFlavor) activeFlavor;
    }

    private <P, R> Stream<R> handleStream(
            String sessionId,
            PromptTemplate template,
            Map<String, Object> variables,
            String tag,
            String testRunId,
            Map<String, Object> mergedLLMParameters,
            Flavor<P, R> activeFlavor,
            P formattedPrompt,
            double start,
            Stream<R> responseStream
    ) {
        AtomicReference<String> aggregatedContent = new AtomicReference<>("");
        return responseStream.
                peek((R chunk) -> {
                    aggregatedContent.getAndUpdate((String previous) -> previous + activeFlavor.getContentFromChunk(chunk));
                    if (activeFlavor.isLastChunk(chunk)) {
                        double end = System.nanoTime() / 1e9;
                        record(
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
                    }
                });
    }

    private void record(
            PromptInfo promptInfo,
            CallInfo callInfo
    ) {
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
            Http.postJsonWithBearer(url, payload, freeplayApiKey);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Unable to record LLM call. Cause: {0}", e.getMessage());
        }
    }

    private Flavor<?, ?> getActiveFlavor(Flavor<?, ?> callFlavor, PromptTemplate prompt) {
        if (callFlavor != null) return callFlavor;
        if (this.clientFlavor != null) return this.clientFlavor;

        String flavorName = prompt.getFlavorName();
        switch (flavorName) {
            case "anthropic_text":
                return new AnthropicFlavor();
            case "openai_text":
                return new OpenAITextFlavor();
            case "openai_chat":
                return new OpenAIChatFlavor();
            default:
                throw new FreeplayException(format("Unable to create Flavor for name '%s'.%n", flavorName));
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

    private void throwIfError(HttpResponse<String> response, int expectedStatus) throws FreeplayException {
        if (response.statusCode() != expectedStatus) {
            if (response.body() != null && response.body().length() > 0) {
                Map<String, Object> bodyMap = Http.parseBody(response);
                Object message = bodyMap.get("message");
                throw new FreeplayException(format("Error calling Freeplay [%s]: %s", response.statusCode(), message));
            } else {
                throw new FreeplayException(format("Error calling Freeplay [%s]", response.statusCode()));
            }
        }
    }

    private static String getFinalTag(String tag) {
        return tag != null ? tag : "latest";
    }

    private String getUrl(String path, Object... args) {
        return format("%s/%s", baseUrl, format(path, args));
    }
}
