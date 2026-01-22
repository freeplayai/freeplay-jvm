package ai.freeplay.client;

public interface ProviderConfig {

    String getApiKey();

    class OpenAIProviderConfig implements ProviderConfig {
        private final String apiKey;

        public OpenAIProviderConfig(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiKey() {
            return apiKey;
        }
    }

    class AnthropicProviderConfig implements ProviderConfig {
        private final String apiKey;

        public AnthropicProviderConfig(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiKey() {
            return apiKey;
        }
    }
}
