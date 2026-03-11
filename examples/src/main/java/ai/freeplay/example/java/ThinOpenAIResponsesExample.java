package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FormattedPrompt;
import ai.freeplay.client.resources.prompts.Prompts.GetFormattedRequest;
import ai.freeplay.client.resources.recordings.CallInfo;
import ai.freeplay.client.resources.recordings.CallInfo.ApiStyle;
import ai.freeplay.client.resources.recordings.RecordPayload;
import ai.freeplay.client.resources.recordings.ResponseInfo;
import ai.freeplay.client.resources.sessions.SessionInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.Freeplay.Config;
import static java.lang.String.format;

/**
 * Example demonstrating the OpenAI Responses API integration with Freeplay.
 *
 * Uses the `openai_responses` flavor which:
 * - Strips system messages (use `instructions` parameter instead)
 * - Wraps messages as {type: "message", role, content}
 * - Formats tool schemas in the flat Responses API style
 * - Supports the `developer` role natively
 */
public class ThinOpenAIResponsesExample {
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

        Map<String, Object> variables = Map.of("location", "San Francisco");

        // Fetch and format the prompt using the openai_responses flavor
        FormattedPrompt<List<Map<String, Object>>> formattedPrompt = fpClient.prompts()
                .<List<Map<String, Object>>>getFormatted(
                        new GetFormattedRequest(projectId, "my-openai-prompt", "latest", variables)
                ).join();

        System.out.println("Instructions (system): " + formattedPrompt.getSystemContent().orElse("none"));
        System.out.println("Input messages: " + formattedPrompt.getFormattedPrompt());
        System.out.println("Tool schema: " + formattedPrompt.getToolSchema());
        System.out.println("Output schema: " + formattedPrompt.getOutputSchema());

        // Build the Responses API call
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", formattedPrompt.getPromptInfo().getModel());
        requestBody.put("input", formattedPrompt.getFormattedPrompt());
        requestBody.putAll(formattedPrompt.getPromptInfo().getModelParameters());

        formattedPrompt.getSystemContent().ifPresent(system ->
                requestBody.put("instructions", system)
        );
        if (formattedPrompt.getToolSchema() != null) {
            requestBody.put("tools", formattedPrompt.getToolSchema());
        }
        if (formattedPrompt.getOutputSchema() != null) {
            requestBody.put("text", Map.of(
                    "format", Map.of(
                            "type", "json_schema",
                            "strict", true,
                            "schema", formattedPrompt.getOutputSchema(),
                            "name", "COTReasoning"
                    )
            ));
        }

        // Call OpenAI Responses API
        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = callOpenAIResponses(openaiApiKey, requestBody);
        long endTime = System.currentTimeMillis();

        JsonNode bodyNode;
        try {
            bodyNode = objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse response body.", e);
        }

        System.out.println("Completion: " + bodyNode);

        // Record to Freeplay — allMessages accepts the output array
        JsonNode outputNode = bodyNode.get("output");
        List<?> output = objectMapper.convertValue(outputNode, List.class);
        List<ChatMessage> allMessages = formattedPrompt.allMessages(output);

        CallInfo callInfo = CallInfo.from(
                formattedPrompt.getPromptInfo(),
                startTime,
                endTime
        ).apiStyle(ApiStyle.BATCH);

        JsonNode usageNode = bodyNode.get("usage");
        if (usageNode != null) {
            callInfo.usage(new CallInfo.UsageTokens(
                    usageNode.path("input_tokens").asInt(),
                    usageNode.path("output_tokens").asInt()
            ));
        }

        SessionInfo sessionInfo = fpClient.sessions().create().getSessionInfo();

        fpClient.recordings().create(
                new RecordPayload(projectId, allMessages)
                        .inputs(variables)
                        .sessionInfo(sessionInfo)
                        .promptVersionInfo(formattedPrompt.getPromptInfo())
                        .callInfo(callInfo)
                        .responseInfo(new ResponseInfo(true))
                        .toolSchema(formattedPrompt.getToolSchema())
        ).join();

        System.out.println("Recording created successfully");
    }

    private static HttpResponse<String> callOpenAIResponses(
            String apiKey,
            Map<String, Object> requestBody
    ) {
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder(new URI("https://api.openai.com/v1/responses"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", format("Bearer %s", apiKey))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
