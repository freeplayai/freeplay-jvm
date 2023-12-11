package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.exceptions.*;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.internal.StringUtils;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.model.*;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.Http.parseBody;
import static ai.freeplay.client.internal.Http.throwLLMIfError;
import static ai.freeplay.client.internal.StringUtils.isBlank;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class OpenAIChatFlavor implements ChatFlavor {

    @Override
    public Provider getProviderEnum() {
        return Provider.OpenAI;
    }

    private static void validateChoices(List<Map<String, Object>> choices) throws FreeplayException {
        if (choices.isEmpty()) {
            throw new LLMServerException("Did not get any 'choices' back from OpenAI.");
        }
    }

    private static void validateParameters(Map<String, Object> llmParameters) {
        if (!llmParameters.containsKey("model")) {
            throw new LLMClientException("The 'model' parameter is required when calling OpenAI");
        }
        if (llmParameters.containsKey("prompt")) {
            throw new LLMClientException("The 'prompt' parameter cannot be specified. It is populated automatically.");
        }
        if (llmParameters.containsKey("messages")) {
            throw new LLMClientException("The 'messages' parameter cannot be specified. It is populated automatically.");
        }
    }

    private Stream<String> callOpenAIStream(
            ProviderConfigs providerConfig,
            String url,
            String promptFieldName,
            Map<String, Object> mergedLLMParameters,
            Collection<ChatMessage> formattedPrompt,
            HttpConfig httpConfig
    ) {
        validateParameters(mergedLLMParameters);
        OpenAIProviderConfig openAIProviderConfig = validateConfig(providerConfig);

        Map<String, Object> bodyMap = new HashMap<>(mergedLLMParameters);
        bodyMap.put(promptFieldName, formattedPrompt);
        bodyMap.put("stream", true);

        HttpResponse<Stream<String>> response;
        try {
            response = Http.postJsonWithBearer(
                    url,
                    bodyMap,
                    openAIProviderConfig.getApiKey(),
                    HttpResponse.BodyHandlers.ofLines(),
                    httpConfig
            );
        } catch (Exception e) {
            throw new LLMServerException("Error calling OpenAI.", e);
        }

        return response.body();
    }

    private IndexedChatMessage parseLine(String line, Function<Map<String, Object>, IndexedChatMessage> itemCreator) {
        String[] field = line.split(":", 2);
        if (field.length == 2 && "data".equals(field[0])) {
            if ("[DONE]".equals(field[1].trim())) {
                return null;
            } else {
                Map<String, Object> objectMap;
                try {
                    objectMap = JSONUtil.parseMap(field[1]);
                } catch (Exception e) {
                    throw new LLMServerException("Error processing OpenAI stream.", e);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) objectMap.get("choices");
                Map<String, Object> firstChoice = choices.get(0);
                return itemCreator.apply(firstChoice);
            }
        } else {
            throw new LLMServerException("Got unknown line in the stream: '" + line + "'");
        }
    }

    private OpenAIProviderConfig validateConfig(ProviderConfigs providerConfig) {
        if (providerConfig.getOpenAIConfig() != null) {
            return providerConfig.getOpenAIConfig();
        } else {
            throw new FreeplayConfigurationException("The OpenAI provider is not configured on the ProviderConfig. " +
                    "Set up this provider config to call OpenAI endpoints.");
        }
    }

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

    @SuppressWarnings("unchecked")
    @Override
    public ChatCompletionResponse callChatService(
            Collection<ChatMessage> formattedMessages,
            ProviderConfigs providerConfig,
            Map<String, Object> llmParameters,
            HttpConfig httpConfig
    ) throws FreeplayException {
        validateParameters(llmParameters);
        OpenAIProviderConfig openAIProviderConfig = validateConfig(providerConfig);

        Map<String, Object> bodyMap = new HashMap<>(llmParameters);
        bodyMap.put("messages", formattedMessages);

        HttpResponse<String> response;
        try {
            response = Http.postJsonWithBearer(OPENAI_CHAT_URL, bodyMap, openAIProviderConfig.getApiKey(), httpConfig);
        } catch (Exception e) {
            throw new LLMServerException("Error calling OpenAI.", e);
        }

        Map<String, Object> responseBody;
        try {
            responseBody = parseBody(response);
        } catch (FreeplayException e) {
            throw new LLMServerException("Error calling OpenAI.", e);
        }
        throwLLMIfError(response, 200);

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
            ProviderConfigs providerConfig,
            Map<String, Object> mergedLLMParameters,
            HttpConfig httpConfig
    ) {
        Stream<String> messages = callOpenAIStream(
                providerConfig,
                OPENAI_CHAT_URL,
                "messages",
                mergedLLMParameters,
                formattedPrompt,
                httpConfig);

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
        try {
            return JSONUtil.asString(formattedMessages);
        } catch (Exception e) {
            throw new LLMServerException("Error processing messages from OpenAI.", e);
        }
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
