package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.internal.StringUtils;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.model.*;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.parseBody;
import static ai.freeplay.client.internal.Http.throwIfError;
import static ai.freeplay.client.internal.StringUtils.isBlank;
import static ai.freeplay.client.internal.StringUtils.isNotBlank;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class AnthropicChatFlavor implements ChatFlavor {

    private static final String ANTHROPIC_COMPLETIONS_URL = "https://api.anthropic.com/v1/complete";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public String getFormatType() {
        return "anthropic_chat";
    }

    @Override
    public Provider getProviderEnum() {
        return Provider.Anthropic;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ChatMessage> formatPrompt(
            String template,
            Map<String, Object> variables
    ) {
        try {
            return JSON.std.listFrom(template).stream().map((Object message) -> {
                Map<String, Object> messageMap = (Map<String, Object>) message;
                String formatted = TemplateUtils.format(valueOf(messageMap.get("content")), variables);
                String role = valueOf(messageMap.get("role"));
                String anthropicRole;
                if ("assistant".equalsIgnoreCase(role)) {
                    anthropicRole = "Assistant";
                } else {
                    anthropicRole = "Human";
                }
                return new ChatMessage(
                        anthropicRole,
                        formatted
                );
            }).collect(toList());
        } catch (IOException e) {
            throw new FreeplayException("Error formatting chat prompt template.", e);
        }
    }

    @Override
    public CompletionResponse callService(
            Collection<ChatMessage> formattedMessages,
            ProviderConfig providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException {
        ChatCompletionResponse chatResponse = callChatService(formattedMessages, providerConfig, mergedLLMParameters, httpConfig);
        return new CompletionResponse(chatResponse.getContent(), chatResponse.isComplete(), true);
    }

    @Override
    public Stream<IndexedChatMessage> callServiceStream(
            Collection<ChatMessage> formattedMessages,
            ProviderConfig providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) {
        validateParameters(mergedLLMParameters);
        Map<String, Object> bodyMap = getRequestBody(formattedMessages, mergedLLMParameters);
        bodyMap.put("stream", true);

        HttpResponse<Stream<String>> httpResponse;
        try {
            httpResponse = Http.postJson(
                    ANTHROPIC_COMPLETIONS_URL,
                    bodyMap,
                    BodyHandlers.ofLines(),
                    httpConfig,
                    "accept", "application/json",
                    "anthropic-version", ANTHROPIC_VERSION,
                    "x-api-key", providerConfig.getApiKey()
            );
        } catch (Exception e) {
            throw new FreeplayException("Error calling Anthropic.", e);
        }

        AtomicReference<StreamState> streamStateRef = new AtomicReference<>(new StreamState());

        return httpResponse.body()
                .map((String line) -> handleLine(line, streamStateRef.get()))
                .filter(Objects::nonNull)
                .map((CompletionResponse completionResponse) -> new IndexedChatMessage(
                        "Assistant",
                        completionResponse.getContent(),
                        0,
                        completionResponse.isComplete(),
                        completionResponse.isLast()
                ));
    }

    @Override
    public ChatCompletionResponse callChatService(
            Collection<ChatMessage> messages,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) throws FreeplayException {
        validateParameters(llmParameters);
        Map<String, Object> bodyMap = getRequestBody(messages, llmParameters);

        HttpResponse<String> response;
        try {
            response = Http.postJson(
                    ANTHROPIC_COMPLETIONS_URL,
                    bodyMap,
                    httpConfig,
                    "accept", "application/json",
                    "anthropic-version", ANTHROPIC_VERSION,
                    "x-api-key", providerConfig.getApiKey()
            );
        } catch (Exception e) {
            throw new FreeplayException("Error calling Anthropic.", e);
        }

        Map<String, Object> responseBody = parseBody(response);
        throwIfError(response, 200);

        boolean isComplete = "stop_sequence".equals(responseBody.get("stop_reason"));
        return new ChatCompletionResponse(List.of(
                new IndexedChatMessage(
                        valueOf(responseBody.get("role")),
                        valueOf(responseBody.get("completion")),
                        0,
                        isComplete,
                        true)
        ));
    }

    @Override
    public String serializeForRecord(Collection<ChatMessage> formattedMessages) {
        return JSONUtil.asString(formattedMessages);
    }

    @Override
    public String getContentFromChunk(IndexedChatMessage chunk) {
        return chunk.getContent();
    }

    @Override
    public boolean isLastChunk(IndexedChatMessage chunk) {
        return chunk.isLast();
    }


    @Override
    public boolean isComplete(IndexedChatMessage chunk) {
        return chunk.isComplete();
    }

    private static Map<String, Object> getRequestBody(Collection<ChatMessage> messages, Map<String, Object> llmParameters) {
        Map<String, Object> bodyMap = new HashMap<>(llmParameters);
        List<String> formattedMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            String content = message.getContent();
            // Anthropic does not support system role for now.
            String role = message.getRole().equals("assistant") ? "Assistant" : "Human";
            formattedMessages.add(role + ": " + content);
        }
        formattedMessages.add("Assistant:");
        bodyMap.put("prompt", "\n\n" + String.join("\n\n", formattedMessages));
        return bodyMap;
    }

    private static void validateParameters(Map<String, Object> llmParameters) {
        if (!llmParameters.containsKey("model")) {
            throw new FreeplayException("The 'model' parameter is required when calling Anthropic.");
        }
        if (!llmParameters.containsKey("max_tokens_to_sample")) {
            throw new FreeplayException("The 'max_tokens_to_sample' parameter is required when calling Anthropic.");
        }
        if (llmParameters.containsKey("prompt")) {
            throw new FreeplayException("The 'prompt' parameter cannot be specified. It is populated automatically.");
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
                Map<String, Object> objectMap = JSONUtil.parseMap(fieldValue);

                streamState.appendData((String) objectMap.get("completion"));
                streamState.setStopReason((String) objectMap.get("stop_reason"));

                return null;
            } else if ("event".equals(fieldName)) {
                streamState.startEvent(fieldValue);
            } else {
                throw new FreeplayException("Got unknown field in the stream: '" + fieldName + "'");
            }
        } else {
            throw new FreeplayException("Got unknown line in the stream: '" + line + "'");
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
                    throw new FreeplayException(
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
