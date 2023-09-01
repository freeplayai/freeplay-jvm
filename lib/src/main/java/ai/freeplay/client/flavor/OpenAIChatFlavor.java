package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.IndexedChatMessage;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.parseBody;
import static ai.freeplay.client.internal.Http.throwIfError;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class OpenAIChatFlavor extends OpenAIFlavor implements ChatFlavor {

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
            Map<String, Object> llmParameters
    ) throws FreeplayException {
        ChatCompletionResponse chatResponse = callChatService(formattedMessages, providerConfig, llmParameters);
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
    public Stream<ChatMessage> callServiceStream(
            Collection<ChatMessage> formattedPrompt,
            ProviderConfig providerConfig,
            Map<String, Object> mergedLLMParameters
    ) {
        return callOpenAIStream(
                providerConfig,
                OPENAI_CHAT_URL,
                "messages",
                mergedLLMParameters,
                formattedPrompt,
                Http.ResponseHandlers.chatItemCreator());
    }

    @Override
    public String getContentFromChunk(ChatMessage chunk) {
        return chunk.getContent();
    }

    @Override
    public boolean isLastChunk(ChatMessage chunk) {
        if (chunk instanceof IndexedChatMessage) {
            return ((IndexedChatMessage) chunk).isLast();
        } else {
            return true;
        }
    }

    @Override
    public boolean isComplete(ChatMessage chunk) {
        if (chunk instanceof IndexedChatMessage) {
            return ((IndexedChatMessage) chunk).isComplete();
        }
        return true;
    }

    @Override
    public String serializeForRecord(Collection<ChatMessage> formattedMessages) {
        return JSONUtil.asString(formattedMessages);
    }

    private static void validateChoices(List<Map<String, Object>> choices) throws FreeplayException {
        if (choices.isEmpty()) {
            throw new FreeplayException("Did not get any 'choices' back from OpenAI.");
        }
    }
}
