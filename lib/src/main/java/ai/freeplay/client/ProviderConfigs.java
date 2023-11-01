package ai.freeplay.client;

import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;

public class ProviderConfigs {

    private final OpenAIProviderConfig openAIConfig;
    private final AnthropicProviderConfig anthropicConfig;

    public static ProviderConfigs fromGenericConfig(ProviderConfig providerConfig) {
        if (providerConfig instanceof OpenAIProviderConfig) {
            return new ProviderConfigs((OpenAIProviderConfig) providerConfig);
        }
        if (providerConfig instanceof AnthropicProviderConfig) {
            return new ProviderConfigs((AnthropicProviderConfig) providerConfig);
        }
        throw new FreeplayConfigurationException("Unknown provider config of type " + providerConfig.getClass().getName());
    }

    public ProviderConfigs(OpenAIProviderConfig openAIConfig, AnthropicProviderConfig anthropicConfig) {
        this.openAIConfig = openAIConfig;
        this.anthropicConfig = anthropicConfig;
    }

    public ProviderConfigs(OpenAIProviderConfig openAIConfig) {
        this(openAIConfig, null);
    }

    public ProviderConfigs(AnthropicProviderConfig anthropicConfig) {
        this(null, anthropicConfig);
    }

    public OpenAIProviderConfig getOpenAIConfig() {
        return openAIConfig;
    }

    public AnthropicProviderConfig getAnthropicConfig() {
        return anthropicConfig;
    }
}
