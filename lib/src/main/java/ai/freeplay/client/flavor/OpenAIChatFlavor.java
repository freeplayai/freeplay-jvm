package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.internal.StringUtils;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.IndexedChatMessage;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.parseBody;
import static ai.freeplay.client.internal.Http.throwIfError;
import static ai.freeplay.client.internal.StringUtils.isBlank;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class OpenAIChatFlavor extends OpenAIFlavor<Collection<ChatMessage>, IndexedChatMessage> implements ChatFlavor {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String getFormatType() {
        return "openai_chat";
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
            throw new FreeplayException("Error formatting chat prompt template.", e);
        }
    }

    @Override
    public CompletionResponse callService(
            Collection<ChatMessage> formattedMessages,
            ProviderConfig providerConfig,
            Map<String, Object> mergedLLMParameters
    ) throws FreeplayException {
        ChatCompletionResponse chatResponse = callChatService(formattedMessages, providerConfig, mergedLLMParameters);
        return new CompletionResponse(chatResponse.getContent(), chatResponse.isComplete(), true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ChatCompletionResponse callChatService(
            Collection<ChatMessage> formattedMessages,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters
    ) throws FreeplayException {
        validateParameters(llmParameters);

        Map<String, Object> bodyMap = new HashMap<>(llmParameters);
        bodyMap.put("messages", formattedMessages);

        HttpResponse<String> response;
        try {
            response = Http.postJsonWithBearer(OPENAI_CHAT_URL, bodyMap, providerConfig.getApiKey());
        } catch (Exception e) {
            throw new FreeplayException("Error calling OpenAI.", e);
        }

        Map<String, Object> responseBody = parseBody(response);
        throwIfError(response, 200);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        validateChoices(choices);
        List<IndexedChatMessage> choiceMessages = choices.stream().map((Map<String, Object> messageObject) -> {
            Map<String, Object> messageMap = (Map<String, Object>) messageObject.get("message");
            boolean isComplete = "stop".equals(messageObject.get("finish_reason"));
            return new IndexedChatMessage(
                    valueOf(messageMap.get("role")),
                    valueOf(messageMap.get("content")),
                    (Integer) messageObject.get("index"),
                    isComplete
            );
        }).collect(toList());

        return new ChatCompletionResponse(choiceMessages);
    }

    @Override
    public Stream<IndexedChatMessage> callServiceStream(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfig providerConfig,
            Map<String, Object> mergedLLMParameters
    ) {
        Stream<String> messages = callOpenAIStream(
                providerConfig,
                OPENAI_CHAT_URL,
                "messages",
                mergedLLMParameters,
                formattedPrompt);

        AtomicReference<String> role = new AtomicReference<>();
        Function<Map<String, Object>, IndexedChatMessage> itemCreator =
                (Map<String, Object> choice) -> createItem(choice, role);

        return messages
                .filter(StringUtils::isNotBlank)
                .map((String line) -> parseLine(line, itemCreator))
                .filter(Objects::nonNull);
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

    @Override
    public String serializeForRecord(Collection<ChatMessage> formattedMessages) {
        return JSONUtil.asString(formattedMessages);
    }

    private IndexedChatMessage createItem(Map<String, Object> choice, AtomicReference<String> roleReference) {
        @SuppressWarnings("unchecked")
        Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
        if (delta.get("role") != null) {
            roleReference.set(valueOf(delta.get("role")));
        }
        boolean isComplete = "stop".equals(choice.get("finish_reason"));
        Object content = delta.get("content");

        if (isBlank(content) && choice.get("finish_reason") != null) {
            return new IndexedChatMessage(roleReference.get(), "", 0, isComplete, true);
        }

        return new IndexedChatMessage(roleReference.get(), valueOf(content), 0, isComplete, false);
    }
}
