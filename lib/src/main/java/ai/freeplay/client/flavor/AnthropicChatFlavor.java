package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.LLMServerException;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.*;
import static ai.freeplay.client.internal.StringUtils.isBlank;
import static ai.freeplay.client.internal.StringUtils.isNotBlank;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class AnthropicChatFlavor implements ChatFlavor {

    private static final String ANTHROPIC_MESSAGES_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String ANTHROPIC_CONTENT_BLOCK_DELTA_EVENT_TYPE = "content_block_delta";
    private static final String ANTHROPIC_MESSAGE_DELTA_EVENT_TYPE = "message_delta";

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
                return new ChatMessage(
                        valueOf(messageMap.get("role")),
                        formatted
                );
            }).collect(toList());
        } catch (IOException e) {
            throw new FreeplayClientException("Error formatting chat prompt template.", e);
        }
    }

    @Override
    public CompletionResponse callService(
            Collection<ChatMessage> formattedMessages,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) throws FreeplayException {
        ChatCompletionResponse chatResponse = callChatService(formattedMessages, providerConfig, mergedLLMParameters, httpConfig);
        return new CompletionResponse(chatResponse.getContent(), chatResponse.isComplete(), true);
    }

    @Override
    public Stream<IndexedChatMessage> callServiceStream(
            Collection<ChatMessage> formattedMessages,
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) {
        validateParameters(mergedLLMParameters);
        Map<String, Object> bodyMap = getRequestBody(formattedMessages, mergedLLMParameters);
        bodyMap.put("stream", true);

        HttpResponse<Stream<String>> httpResponse;
        try {
            httpResponse = Http.postJson(
                    ANTHROPIC_MESSAGES_URL,
                    bodyMap,
                    BodyHandlers.ofLines(),
                    httpConfig,
                    "accept", "application/json",
                    "anthropic-version", ANTHROPIC_VERSION,
                    "x-api-key", providerConfig.getAnthropicConfig().getApiKey()
            );
        } catch (Exception e) {
            throw new FreeplayException("Error calling Anthropic.", e);
        }

        AtomicReference<StreamState> streamStateRef = new AtomicReference<>(new StreamState());

        return httpResponse.body()
                .map((String line) -> handleLine(line, streamStateRef.get()))
                .filter(Objects::nonNull)
                .map((CompletionResponse completionResponse) -> new IndexedChatMessage(
                        "assistant",
                        completionResponse.getContent(),
                        0,
                        completionResponse.isComplete(),
                        completionResponse.isLast()
                ));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ChatCompletionResponse callChatService(
            Collection<ChatMessage> messages,
            ProviderConfigs providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) throws FreeplayException {
        validateParameters(llmParameters);
        Map<String, Object> bodyMap = getRequestBody(messages, llmParameters);

        HttpResponse<String> response;
        try {
            response = Http.postJson(
                    ANTHROPIC_MESSAGES_URL,
                    bodyMap,
                    httpConfig,
                    "accept", "application/json",
                    "anthropic-version", ANTHROPIC_VERSION,
                    "x-api-key", providerConfig.getAnthropicConfig().getApiKey()
            );
        } catch (Exception e) {
            throw new FreeplayException("Error calling Anthropic.", e);
        }

        throwFreeplayIfError(response, 200);

        Map<String, Object> responseBody;
        try {
            responseBody = parseBody(response);
        } catch (FreeplayException e) {
            throw new LLMServerException("Error calling Anthropic.", e);
        }
        throwLLMIfError(response, 200);

        List<Map<String, Object>> responseContents = (List<Map<String, Object>>) responseBody.get("content");

        AtomicInteger index  = new AtomicInteger(0);
        List<IndexedChatMessage> anthropicMessages = responseContents.stream().map((Map<String, Object> messageObject) -> new IndexedChatMessage(
                valueOf(responseBody.get("role")),
                valueOf(messageObject.get("text")),
                index.getAndIncrement(),
                "stop_sequence".equals(messageObject.get("stop_reason")),
                true
        )).collect(toList());

        return new ChatCompletionResponse(anthropicMessages);
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

    private Map<String, Object> getRequestBody(Collection<ChatMessage> messages, Map<String, Object> llmParameters) {
        Map<String, Object> bodyMap = new HashMap<>(llmParameters);
        Collection<ChatMessage> messagesWithoutSystem = messages
                .stream()
                .filter(msg -> !msg.getRole().equals("system"))
                .collect(toList());
        bodyMap.put("messages", messagesWithoutSystem);

        Optional<String> maybeSystemContent = messages
                .stream()
                .filter(msg -> msg.getRole().equals("system"))
                .findFirst()
                .map(ChatMessage::getContent);
        maybeSystemContent.ifPresent(systemContent -> bodyMap.put("system", systemContent));

        return bodyMap;
    }

    private static void validateParameters(Map<String, Object> llmParameters) {
        if (!llmParameters.containsKey("model")) {
            throw new FreeplayException("The 'model' parameter is required when calling Anthropic.");
        }
        if (!llmParameters.containsKey("max_tokens")) {
            throw new FreeplayException("The 'max_tokens' parameter is required when calling Anthropic.");
        }
        if (llmParameters.containsKey("prompt")) {
            throw new FreeplayException("The 'prompt' parameter cannot be specified. It is populated automatically.");
        }
    }

    @SuppressWarnings("unchecked")
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

                String eventType = (String) objectMap.get("type");
                if (eventType.equals(ANTHROPIC_CONTENT_BLOCK_DELTA_EVENT_TYPE)) {
                    // Text is streamed through content block delta events.
                    Map<String, Object> delta = (Map<String, Object>) objectMap.get("delta");
                    streamState.appendData((String) delta.get("text"));
                } else if (eventType.equals(ANTHROPIC_MESSAGE_DELTA_EVENT_TYPE)) {
                    // Stop reason is streamed through message delta events, after a message is complete.
                    Map<String, Object> delta = (Map<String, Object>) objectMap.get("delta");
                    streamState.setStopReason((String) delta.get("stop_reason"));
                }
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
        if (ANTHROPIC_CONTENT_BLOCK_DELTA_EVENT_TYPE.equals(streamState.eventName) || ANTHROPIC_MESSAGE_DELTA_EVENT_TYPE.equals(streamState.eventName)) {
            return streamState.closeCurrentEvent();
        } else {
            // Anthropic indicates we need to be tolerant of new event types.
            // So we simply ignore pings and any new event types.
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
