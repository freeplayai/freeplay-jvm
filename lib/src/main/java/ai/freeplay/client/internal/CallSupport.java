package ai.freeplay.client.internal;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.flavor.OpenAITextFlavor;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.PromptTemplate;
import ai.freeplay.client.model.Session;

import java.net.http.HttpResponse;
import java.util.*;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class CallSupport {
    // Using the Freeplay class to make it nicer, since these will be in customer application logs
    public static final System.Logger LOGGER = System.getLogger(Freeplay.class.getName());

    private final String freeplayApiKey;
    private final String baseUrl;
    private final Flavor<?> flavor;
    private final Map<String, Object> clientLLMParameters;
    private final ProviderConfig providerConfig;

    public CallSupport(
            String freeplayApiKey,
            String baseUrl,
            ProviderConfig providerConfig,
            Flavor<?> flavor,
            Map<String, Object> llmParameters
    ) {
        this.freeplayApiKey = freeplayApiKey;
        this.baseUrl = baseUrl;
        this.providerConfig = providerConfig;
        this.flavor = flavor;
        this.clientLLMParameters = llmParameters != null ? llmParameters : Collections.emptyMap();
    }

    public Session createSession(String projectId, String tag) throws FreeplayException {
        String finalTag = getFinalTag(tag);
        String url = getUrl("projects/%s/sessions/tag/%s", projectId, finalTag);

        HttpResponse<String> response = HttpUtil.postWithBearer(url, freeplayApiKey);
        throwIfError(response, 201);

        Map<String, Object> sessionMap = HttpUtil.parseBody(response);
        return new Session(String.valueOf(sessionMap.get("session_id")));
    }

    @SuppressWarnings("unchecked")
    public Collection<PromptTemplate> getPrompts(String projectId, String tag) throws FreeplayException {
        String finalTag = getFinalTag(tag);
        String url = getUrl("projects/%s/templates/all/%s", projectId, finalTag);
        HttpResponse<String> response = HttpUtil.get(url, freeplayApiKey);
        throwIfError(response, 200);

        Map<String, Object> templatesMap = HttpUtil.parseBody(response);
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

    @SuppressWarnings("unchecked")
    public <P> CompletionResponse prepareAndMakeCall(
            Collection<PromptTemplate> prompts,
            String sessionId,
            String templateName,
            Map<String, Object> variables,
            Map<String, Object> llmParameters,
            String tag,
            String testRunId,
            Flavor<P> flavor
    ) throws FreeplayException {
        Optional<PromptTemplate> maybePrompt = findPrompt(prompts, templateName);
        if (maybePrompt.isEmpty()) {
            throw new FreeplayException(
                    "Prompt template " + templateName + " in environment " + tag + " not found.");
        }
        PromptTemplate prompt = maybePrompt.get();

        Map<String, Object> mergedLLMParameters = getMergedParameters(prompt, llmParameters);
        Flavor<P> activeFlavor = (Flavor<P>) getActiveFlavor(flavor, prompt);

        // Once we do chat consider whether coercing to a String is what we want
        String formattedPrompt = String.valueOf(activeFlavor.formatPrompt(prompt.getContent(), variables));

        long start = System.currentTimeMillis();
        CompletionResponse response = activeFlavor.callService(formattedPrompt, providerConfig, mergedLLMParameters);
        long end = System.currentTimeMillis();

        record(
                new PromptInfo(
                        prompt.getPromptTemplateVersionId(),
                        prompt.getPromptTemplateId(),
                        activeFlavor.getFormatType(),
                        activeFlavor.getProvider(),
                        String.valueOf(mergedLLMParameters.get("model")),
                        mergedLLMParameters
                ),
                new CallInfo(
                        sessionId,
                        testRunId,
                        start,
                        end,
                        tag,
                        variables,
                        formattedPrompt,
                        response.getContent(),
                        response.isComplete()
                )
        );
        return response;
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
            HttpUtil.postJsonWithBearer(url, payload, freeplayApiKey);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Unable to record LLM call. Cause: {0}", e.getMessage());
        }
    }

    private Map<String, Object> getMergedParameters(PromptTemplate promptTemplate, Map<String, Object> callLLMParameters) {
        Map<String, Object> merged = new HashMap<>(16);
        merged.putAll(promptTemplate.getLLMParameters());
        merged.putAll(clientLLMParameters);
        merged.putAll(callLLMParameters);
        return merged;
    }

    private void throwIfError(HttpResponse<String> response, int expectedStatus) throws FreeplayException {
        if (response.statusCode() != expectedStatus) {
            if (response.body() != null && response.body().length() > 0) {
                Map<String, Object> bodyMap = HttpUtil.parseBody(response);
                Object message = bodyMap.get("message");
                throw new FreeplayException(format("Error calling Freeplay [%s]: %s", response.statusCode(), message));
            } else {
                throw new FreeplayException(format("Error calling Freeplay [%s]", response.statusCode()));
            }
        }
    }

    private Flavor<?> getActiveFlavor(Flavor<?> flavor, PromptTemplate prompt) {
        if (flavor != null) return flavor;
        if (this.flavor != null) return this.flavor;

        String flavorName = prompt.getFlavorName();
        switch (flavorName) {
            case "openai_text":
                return new OpenAITextFlavor();
            default:
                throw new FreeplayException(format("Unable to create Flavor for name '%s'.%n", flavorName));
        }
    }

    private static String getFinalTag(String tag) {
        return tag != null ? tag : "latest";
    }

    private String getUrl(String path, Object... args) {
        return format("%s/%s", baseUrl, format(path, args));
    }

    private Optional<PromptTemplate> findPrompt(Collection<PromptTemplate> templates, String templateName) {
        return templates.stream()
                .filter(template -> template.getName().equals(templateName))
                .findFirst();
    }
}
