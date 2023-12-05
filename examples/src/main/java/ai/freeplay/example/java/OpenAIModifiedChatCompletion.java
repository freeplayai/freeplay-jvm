package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.model.ChatCompletionResponse;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.processor.ChatPromptProcessor;
import ai.freeplay.client.processor.LLMCallInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class OpenAIModifiedChatCompletion {

    private static final AtomicInteger totalCharCount = new AtomicInteger(0);

    public static void main(String[] args) {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");

        String baseUrl = format("https://%s.freeplay.ai/api", customerDomain);

        Freeplay fpClient = new Freeplay(
                freeplayApiKey,
                baseUrl,
                new ProviderConfigs(new OpenAIProviderConfig(openaiApiKey))
        );
        Map<String, Object> llmParameters = Collections.emptyMap();

        CompletionSession session = fpClient.createSession(projectId, "prod");

        ChatCompletionResponse chatResponse = session.getChatCompletion(
                "my-chat-start",
                Map.of("question", "why isn't my sink working?"),
                llmParameters,
                CHAT_PROMPT_PROCESSOR
        );
        chatResponse.getFirstChoice().ifPresent((ChatMessage message) ->
                System.out.printf("Chat Completion text [%s]: %s%n", message.getRole(), chatResponse.getContent())
        );

        System.out.printf("We sent a total of %s characters to the LLMs.%n", totalCharCount.get());
    }

    private static final ChatPromptProcessor CHAT_PROMPT_PROCESSOR = (Collection<ChatMessage> messages, LLMCallInfo info) -> {
        System.out.printf("Calling '%s' with model '%s'%n",
                info.getProvider().getFriendlyName(),
                info.getLLMParameters().get("model"));

        List<ChatMessage> newMessages = new ArrayList<>(messages);
        newMessages.add(1, new ChatMessage("user", "Please help me as best you can without making anything up."));

        totalCharCount.getAndAdd(
                newMessages.stream()
                        .mapToInt((ChatMessage message) -> message.getContent().length())
                        .sum());

        return newMessages;
    };
}
