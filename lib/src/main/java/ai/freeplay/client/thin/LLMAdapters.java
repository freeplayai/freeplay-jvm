package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

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

    public static class AnthropicLLMAdapter implements LLMAdapter<String> {
        @Override
        public String getProvider() {
            return "anthropic";
        }

        @Override
        public String toLLMSyntax(List<ChatMessage> messages) {
            List<String> formattedMessages = new ArrayList<>();
            for (ChatMessage message : messages) {
                String content = message.getContent();
                // Anthropic does not support system role for now.
                String role = message.getRole().equals("assistant") ? "Assistant" : "Human";
                formattedMessages.add(role + ": " + content);
            }
            formattedMessages.add("Assistant:");
            return "\n\n" + String.join("\n\n", formattedMessages);
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
