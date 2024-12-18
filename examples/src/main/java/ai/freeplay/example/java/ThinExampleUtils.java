package ai.freeplay.example.java;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

@SuppressWarnings("unused")
public class ThinExampleUtils {
    public static class Tuple2<Type1, Type2> {
        public final Type1 first;
        public final Type2 second;

        public Tuple2(Type1 first, Type2 second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Tuple3<Type1, Type2, Type3> {
        public final Type1 first;
        public final Type2 second;
        public final Type3 third;

        public Tuple3(Type1 first, Type2 second, Type3 third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }

    public static CompletableFuture<HttpResponse<String>> callOpenAI(
            ObjectMapper objectMapper,
            String openAIApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages
    ) {
        return callOpenAI(objectMapper, openAIApiKey, model, llmParameters, messages, null, null);
    }

    public static CompletableFuture<HttpResponse<String>> callOpenAI(
            ObjectMapper objectMapper,
            String openAIApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages,
            List<OpenAIFunctionCallDTO> functionCalls
    ) {
        return callOpenAI(objectMapper, openAIApiKey, model, llmParameters, messages, functionCalls, null);
    }

    public static CompletableFuture<HttpResponse<String>> callOpenAIWithTools(
            ObjectMapper objectMapper,
            String openAIApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages,
            List<Map<String, Object>> toolSchema
    ) {
        return callOpenAI(objectMapper, openAIApiKey, model, llmParameters, messages, null, toolSchema);
    }

    public static CompletableFuture<HttpResponse<String>> callOpenAI(
            ObjectMapper objectMapper,
            String openAIApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages,
            List<OpenAIFunctionCallDTO> functionCalls,
            List<Map<String, Object>> toolSchema
    ) {
        try {
            String openAIChatURL = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("messages", messages);
            if (functionCalls != null && !functionCalls.isEmpty()) {
                bodyMap.put("functions", functionCalls);
            }
            if (toolSchema != null) {
                bodyMap.put("tools", toolSchema);
            }
            bodyMap.putAll(llmParameters);
            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(new URI(openAIChatURL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", format("Bearer %s", openAIApiKey))
                    .POST(ofString(body));

            return HttpClient.newBuilder()
                    .build()
                    .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<HttpResponse<String>> callAnthropic(
            ObjectMapper objectMapper,
            String anthropicApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages,
            String systemContent
    ) {
        return callAnthropic(objectMapper, anthropicApiKey, model, llmParameters, messages, systemContent, Collections.emptyList());
    }

    public static CompletableFuture<HttpResponse<String>> callAnthropic(
            ObjectMapper objectMapper,
            String anthropicApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages,
            String systemContent,
            List<Map<String, Object>> toolSchema
    ) {
        try {
            String anthropicMessagesURL = "https://api.anthropic.com/v1/messages";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("messages", messages);
            if (systemContent != null) {
                bodyMap.put("system", systemContent);
            }

            if (toolSchema != null) {
                bodyMap.put("tools", toolSchema);

            }
            bodyMap.putAll(llmParameters);
            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(new URI(anthropicMessagesURL))
                    .header("accept", "application/json")
                    .header("anthropic-version", "2023-06-01")
                    .header("x-api-key", anthropicApiKey)
                    .POST(ofString(body));

            return HttpClient.newBuilder()
                    .build()
                    .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<HttpResponse<String>> callBaseten(
            ObjectMapper objectMapper,
            String basetenApiKey,
            String modelId,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages
    ) {
        try {
            String modelUrl = format("https://model-%s.api.baseten.co/production/predict", modelId);

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("messages", messages);
            bodyMap.putAll(llmParameters);
            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(new URI(modelUrl))
                    .header("accept", "application/json")
                    .header("Authorization", format("Api-Key %s", basetenApiKey))
                    .POST(ofString(body));

            return HttpClient.newBuilder()
                    .build()
                    .sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OpenAIFunctionCallDTO {
        private final String name;
        private final String description;
        private final Parameters parameters;

        public OpenAIFunctionCallDTO(String name, String description, Parameters parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Parameters getParameters() {
            return parameters;
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Parameters {
            private final String type;
            private final Map<String, Property> properties;

            public Parameters(String type, Map<String, Property> properties) {
                this.type = type;
                this.properties = properties;
            }

            public String getType() {
                return type;
            }

            public Map<String, Property> getProperties() {
                return properties;
            }
        }

        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Property {
            private final String type;
            private final String description;

            public Property(String type, String description) {
                this.type = type;
                this.description = description;
            }

            public String getType() {
                return type;
            }

            public String getDescription() {
                return description;
            }
        }
    }
}
