package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.CallInfo.ApiStyle;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.sessions.SessionInfo;
import ai.freeplay.example.java.ThinExampleUtils.Tuple3;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.lang.String.format;

/**
 * Example demonstrating structured output support with OpenAI.
 *
 * This example shows how to:
 * 1. Define an output schema for structured responses
 * 2. Call OpenAI with the output schema
 * 3. Record the interaction including the output schema
 */
public class ThinOpenAIStructuredOutputExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String apiRoot = System.getenv("FREEPLAY_API_URL");
        String baseUrl = format("%s/api", apiRoot);
        String openaiApiKey = System.getenv("OPENAI_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .baseUrl(baseUrl)
        );

        Map<String, Object> variables = Map.of("question", "what is quantum computing");

        fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        projectId,
                        "my-chat-template",
                        "latest",
                        variables,
                        null
                ).thenCompose((FormattedPrompt<List<ChatMessage>> formattedPrompt) -> {
                            long startTime = System.currentTimeMillis();

                            // Call OpenAI with structured output schema
                            return callOpenAIWithStructuredOutput(
                                    objectMapper,
                                    openaiApiKey,
                                    formattedPrompt.getPromptInfo().getModel(),
                                    formattedPrompt.getPromptInfo().getModelParameters(),
                                    formattedPrompt.getFormattedPrompt(),
                                    formattedPrompt.getOutputSchema()
                            ).thenApply((HttpResponse<String> response) ->
                                    new Tuple3<>(formattedPrompt, response, startTime)
                            );
                        }
                ).thenCompose((Tuple3<FormattedPrompt<List<ChatMessage>>, HttpResponse<String>, Long> promptAndResponse) -> {
                            FormattedPrompt<List<ChatMessage>> formattedPrompt = promptAndResponse.first;
                            HttpResponse<String> response = promptAndResponse.second;
                            long startTime = promptAndResponse.third;

                            JsonNode bodyNode;
                            try {
                                bodyNode = objectMapper.readTree(response.body());
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException("Unable to parse response body.", e);
                            }

                            JsonNode choicesNode = bodyNode.get("choices");
                            JsonNode messageNode = choicesNode.get(0).get("message");

                            Object message = objectMapper.convertValue(messageNode, Object.class);

                            List<ChatMessage> allMessages = formattedPrompt.allMessages(message);

                            CallInfo callInfo = CallInfo.from(
                                    formattedPrompt.getPromptInfo(),
                                    startTime,
                                    System.currentTimeMillis()
                            ).apiStyle(ApiStyle.BATCH);
                            ResponseInfo responseInfo = new ResponseInfo(
                                    "stop".equals(bodyNode.path("choices").get(0).path("finish_reason").asText())
                            );
                            SessionInfo sessionInfo = fpClient.sessions().create()
                                    .customMetadata(Map.of("custom_field", "structured_output_example"))
                                    .getSessionInfo();

                            System.out.println("Structured output received: " + messageNode.get("content").asText());

                            // Record the interaction including the output schema
                            return fpClient.recordings().create(
                                    new RecordInfo(
                                            projectId,
                                            allMessages
                                    ).inputs(variables)
                                            .sessionInfo(sessionInfo)
                                            .promptVersionInfo(formattedPrompt.getPromptInfo())
                                            .callInfo(callInfo)
                                            .responseInfo(responseInfo)
                                            .outputSchema(formattedPrompt.getOutputSchema()));
                        }
                )
                .exceptionally(exception -> {
                    System.out.println("Got exception: " + exception.getMessage());
                    exception.printStackTrace();
                    return new RecordResponse(null);
                })
                .join();
    }

    /**
     * Calls OpenAI API with structured output schema support.
     *
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     * @param openaiApiKey OpenAI API key
     * @param model Model to use (e.g., "gpt-4o-mini")
     * @param modelParameters Additional model parameters
     * @param messages Messages to send
     * @param outputSchema JSON schema defining the expected output structure
     * @return CompletableFuture with the HTTP response
     */
    private static CompletableFuture<HttpResponse<String>> callOpenAIWithStructuredOutput(
            ObjectMapper objectMapper,
            String openaiApiKey,
            String model,
            Map<String, Object> modelParameters,
            List<ChatMessage> messages,
            Map<String, Object> outputSchema
    ) {
        Map<String, Object> body = new HashMap<>(modelParameters);
        body.put("model", model);
        body.put("messages", messages);

        // Add response_format with the output schema if provided
        if (outputSchema != null && !outputSchema.isEmpty()) {
            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_schema");
            Map<String, Object> jsonSchema = new HashMap<>();
            jsonSchema.put("name", "structured_output");
            jsonSchema.put("schema", outputSchema);
            jsonSchema.put("strict", true);
            responseFormat.put("json_schema", jsonSchema);
            body.put("response_format", responseFormat);
        }

        String bodyString;
        try {
            bodyString = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Unable to serialize request body", e));
            return future;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                .build();

        return HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
