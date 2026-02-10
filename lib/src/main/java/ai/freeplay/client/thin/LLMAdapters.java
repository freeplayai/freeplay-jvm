package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.thin.resources.prompts.*;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class LLMAdapters {
    public interface LLMAdapter<LLMFormat> {
        String getProvider();
        LLMFormat toLLMSyntax(List<ChatMessage> messages);

        default List<Map<String, Object>> toToolSchemaFormat(List<ToolSchema> toolSchema) {
            throw new UnsupportedOperationException("Tool schema format not supported for this model and provider.");
        }

        default Map<String, Object> toOutputSchemaFormat(Map<String, Object> outputSchema) {
            throw new FreeplayConfigurationException("Structured outputs are not supported for this model and provider.");
        }
    }

    public static LLMAdapter<?> adapterForFlavor(String flavor) {
        switch (flavor) {
            case "openai_chat":
                return new OpenAILLMAdapter();
            case "anthropic_chat":
                return new AnthropicLLMAdapter();
            case "llama_3_chat":
                return new Llama3LLMAdapter();
            case "baseten_mistral_chat":
                return new BasetenLLMAdapter();
            case "gemini_chat":
                return new GeminiLLMAdapter();
            case "gemini_api_chat":
                return new GeminiApiLLMAdapter();
            case "amazon_bedrock_converse":
            case "bedrock_converse":
                return new BedrockConverseAdapter();
            default:
                throw new FreeplayConfigurationException(format("Unable to create LLMAdapter for name '%s'.%n", flavor));
        }
    }

    public static class Llama3LLMAdapter implements LLMAdapter<String> {
        @Override
        public String getProvider() {
            return "sagemaker";
        }

        @Override
        public String toLLMSyntax(List<ChatMessage> messages) {
            return "<|begin_of_text|>" +
                    messages
                            .stream()
                            .map(message -> format(
                                            "<|start_header_id|>%s<|end_header_id|>\n%s<|eot_id|>",
                                            message.getRole(),
                                            message.getContent()
                                    )
                            )
                            .collect(joining("")) +
                    "<|start_header_id|>assistant<|end_header_id|>";
        }
    }

    public static class BasetenLLMAdapter implements LLMAdapter<List<ChatMessage>> {
        @Override
        public String getProvider() {
            return "baseten";
        }

        @Override
        public List<ChatMessage> toLLMSyntax(List<ChatMessage> messages) {
            return messages;
        }
    }
}
