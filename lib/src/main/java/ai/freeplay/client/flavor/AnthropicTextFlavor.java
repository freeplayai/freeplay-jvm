package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.LLMClientException;
import ai.freeplay.client.exceptions.LLMServerException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.internal.StringUtils;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.Provider;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.*;
import static ai.freeplay.client.internal.StringUtils.isBlank;
import static ai.freeplay.client.internal.StringUtils.isNotBlank;
import static java.lang.String.format;
import static java.lang.String.valueOf;

public class AnthropicTextFlavor implements Flavor<String, CompletionResponse> {

    private static final String ANTHROPIC_COMPLETIONS_URL = "https://api.anthropic.com/v1/complete";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public String getFormatType() {
        return "anthropic_text";
    }

    @Override
    public Provider getProviderEnum() {
        return Provider.Anthropic;
    }

    @Override
    public String formatPrompt(
            String template,
            Map<String, Object> variables
    ) {
        return TemplateUtils.format(template, variables);
    }

    @Override
    public CompletionResponse callService(
            String formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException {
        validateParameters(mergedLLMParameters);
        AnthropicProviderConfig anthropicProviderConfig = validateConfig(providerConfig);

        Map<String, Object> bodyMap = getRequestBody(formattedPrompt, mergedLLMParameters);

        HttpResponse<String> response;
        try {
            response = Http.postJson(
                    ANTHROPIC_COMPLETIONS_URL,
                    bodyMap,
                    httpConfig,
                    "accept", "application/json",
                    "anthropic-version", ANTHROPIC_VERSION,
                    "x-api-key", anthropicProviderConfig.getApiKey()
            );
        } catch (Exception e) {
            throw new LLMServerException("Error calling Anthropic.", e);
        }

        Map<String, Object> responseBody;
        try {
            responseBody = parseBody(response);
        } catch (FreeplayException e) {
            throw new LLMServerException("Error calling Anthropic.", e);
        }
        throwLLMIfError(response, 200);

        boolean isComplete = "stop_sequence".equals(responseBody.get("stop_reason"));
        return new CompletionResponse(valueOf(responseBody.get("completion")), isComplete, true);
    }

    @Override
    public Stream<CompletionResponse> callServiceStream(
            String formattedPrompt,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) {
        validateParameters(mergedLLMParameters);
        AnthropicProviderConfig anthropicProviderConfig = validateConfig(providerConfig);

        Map<String, Object> bodyMap = getRequestBody(formattedPrompt, mergedLLMParameters);
        bodyMap.put("stream", true);

        HttpResponse<Stream<String>> response;
        try {
            response = Http.postJson(
                    ANTHROPIC_COMPLETIONS_URL,
                    bodyMap,
                    BodyHandlers.ofLines(),
                    httpConfig,
                    "accept", "application/json",
                    "anthropic-version", ANTHROPIC_VERSION,
                    "x-api-key", anthropicProviderConfig.getApiKey()
            );
        } catch (Exception e) {
            throw new LLMServerException("Error calling Anthropic.", e);
        }

        AtomicReference<StreamState> streamStateRef = new AtomicReference<>(new StreamState());

        return response.body()
                .map((String line) -> handleLine(line, streamStateRef.get()))
                .filter(Objects::nonNull);
    }

    @Override
    public String serializeForRecord(String formattedPrompt) {
        return formattedPrompt;
    }

    @Override
    public String getContentFromChunk(CompletionResponse chunk) {
        return chunk.getContent();
    }

    @Override
    public boolean isLastChunk(CompletionResponse chunk) {
        return chunk.isLast();
    }

    @Override
    public boolean isComplete(CompletionResponse chunk) {
        return chunk.isComplete();
    }

    private static Map<String, Object> getRequestBody(String formattedPrompt, Map<String, Object> llmParameters) {
        Map<String, Object> bodyMap = new HashMap<>(llmParameters);
        bodyMap.put("prompt", toAnthropicPrompt(formattedPrompt));
        return bodyMap;
    }

    private static String toAnthropicPrompt(String formattedPrompt) {
        return format("\n\nHuman: %s \n\nAssistant:", formattedPrompt);
    }

    private static void validateParameters(Map<String, Object> llmParameters) {
        if (!llmParameters.containsKey("model")) {
            throw new LLMClientException("The 'model' parameter is required when calling Anthropic.");
        }
        if (!llmParameters.containsKey("max_tokens_to_sample")) {
            throw new LLMClientException("The 'max_tokens_to_sample' parameter is required when calling Anthropic.");
        }
        if (llmParameters.containsKey("prompt")) {
            throw new LLMClientException("The 'prompt' parameter cannot be specified. It is populated automatically.");
        }
    }

    private CompletionResponse handleLine(String line, StreamState streamState) {
        // We're implementing this spec in this method:
        //  https://html.spec.whatwg.org/multipage/server-sent-events.html#event-stream-interpretation

        if (StringUtils.isBlank(line)) {
            return finishEvent(streamState);
        }

        String[] fields = line.split(":", 2);
        if (fields.length == 2) {
            String fieldName = fields[0].trim();
            String fieldValue = fields[1].stripLeading();

            if ("data".equals(fieldName)) {
                if (isBlank(fieldValue.trim())) {
                    return null;
                }
                Map<String, Object> objectMap;
                try {
                    objectMap = JSONUtil.parseMap(fieldValue);
                } catch (Exception e) {
                    throw new LLMServerException("Error processing Anthropic stream. ", e);
                }

                streamState.appendData((String) objectMap.get("completion"));
                streamState.setStopReason((String) objectMap.get("stop_reason"));

                return null;
            } else if ("event".equals(fieldName)) {
                streamState.startEvent(fieldValue);
            } else {
                throw new LLMServerException("Got unknown field in the stream: '" + fieldName + "'");
            }
        } else {
            throw new LLMServerException("Got unknown line in the stream: '" + line + "'");
        }
        return null;
    }

    private static CompletionResponse finishEvent(StreamState streamState) {
        if ("completion".equals(streamState.eventName)) {
            return streamState.closeCurrentEvent();
        } else {
            // Currently we only expect 'completion' and 'ping' event types, but Anthropic indicates we need
            // to be tolerant of new event types. So we simply ignore pings and any new event types.
            streamState.reset();
            return null;
        }
    }

    private AnthropicProviderConfig validateConfig(ProviderConfigs providerConfig) {
        if (providerConfig.getAnthropicConfig() != null) {
            return providerConfig.getAnthropicConfig();
        } else {
            throw new FreeplayConfigurationException("The Anthropic provider is not configured on the ProviderConfig. " +
                    "Set up this provider config to call Anthropic endpoints.");
        }
    }

    private static class StreamState {
        private final Object lock = new Object();

        private String eventName;
        private StringBuilder data;
        private String stopReason;
        private boolean isComplete;

        private StreamState() {
            reset();
        }

        private void startEvent(String name) {
            synchronized (lock) {
                if (eventName != null) {
                    throw new LLMServerException(
                            format("Attempting to start a new event (%s) when the previous has not been closed.%n", name));
                }
                eventName = name;
            }
        }

        private void appendData(String newData) {
            if (newData != null) {
                synchronized (lock) {
                    data.append(newData);
                }
            }
        }

        private CompletionResponse closeCurrentEvent() {
            synchronized (lock) {
                boolean isLast = stopReason != null;
                CompletionResponse response = new CompletionResponse(data.toString(), isComplete, isLast);

                reset();

                return response;
            }
        }

        private void reset() {
            synchronized (lock) {
                eventName = null;
                data = new StringBuilder();
                stopReason = null;
                isComplete = false;
            }
        }

        public void setStopReason(String stopReason) {
            synchronized (lock) {
                this.stopReason = stopReason;
                if (isNotBlank(stopReason) && "stop_sequence".equals(stopReason)) {
                    isComplete = true;
                }
            }
        }
    }
}
