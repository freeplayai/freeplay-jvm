package ai.freeplay.example.java;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private static CompletableFuture<HttpResponse<String>> callOpenAI(
            ObjectMapper objectMapper,
            String openAIApiKey,
            String model,
            Map<String, Object> llmParameters,
            List<ChatMessage> messages
    ) {
        try {
            String openAIChatURL = "https://api.openai.com/v1/chat/completions";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("messages", messages);
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
            String prompt
    ) {
        try {
            String anthropicCompletionsURL = "https://api.anthropic.com/v1/complete";

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("model", model);
            bodyMap.put("prompt", prompt);
            bodyMap.putAll(llmParameters);
            String body = objectMapper.writeValueAsString(bodyMap);

            HttpRequest.Builder requestBuilder = HttpRequest
                    .newBuilder(new URI(anthropicCompletionsURL))
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
}
