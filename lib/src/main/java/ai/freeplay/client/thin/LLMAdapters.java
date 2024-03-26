package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class LLMAdapters {
    public interface LLMAdapter<LLMFormat> {
        String getProvider();
        LLMFormat toLLMSyntax(List<ChatMessage> messages);
    }

    public static LLMAdapter<?> adapterForFlavor(String flavor) {
        switch (flavor) {
            case "openai_chat":
                return new OpenAILLMAdapter();
            case "anthropic_chat":
                return new AnthropicLLMAdapter();
            default:
                throw new FreeplayConfigurationException(format("Unable to create LLMAdapter for name '%s'.%n", flavor));
        }
    }

    public static class AnthropicLLMAdapter implements LLMAdapter<List<ChatMessage>> {
        @Override
        public String getProvider() {
            return "anthropic";
        }

        @Override
        public List<ChatMessage> toLLMSyntax(List<ChatMessage> messages) {
            return messages
                    .stream()
                    .filter(message -> !message.getRole().equals("system"))
                    .collect(toList());
        }
    }

    public static class OpenAILLMAdapter implements LLMAdapter<List<ChatMessage>> {
        @Override
        public String getProvider() {
            return "openai";
        }

        @Override
        public List<ChatMessage> toLLMSyntax(List<ChatMessage> messages) {
            return messages;
        }
    }
}
