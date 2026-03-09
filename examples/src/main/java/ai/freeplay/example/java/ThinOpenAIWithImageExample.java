package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.Prompts.GetFormattedRequest;
import ai.freeplay.client.resources.recordings.CallInfo;
import ai.freeplay.client.resources.recordings.RecordPayload;
import ai.freeplay.client.resources.recordings.ResponseInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.example.java.ExampleUtils.callOpenAI;

public class ThinOpenAIWithImageExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String baseUrl = System.getenv("FREEPLAY_API_URL");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .baseUrl(baseUrl)
        );

        Map<String, Object> variables = Map.of("question", "Will you describe this image?");

        var prompt = fpClient.prompts()
                .getFormatted(new GetFormattedRequest(projectId, "media", "latest", variables))
                .get();

        var messages = prompt.allMessages(
                new ChatMessage("user", List.of(Map.of(
                        "type", "image_url",
                        "image_url", Map.of(
                                "url", "https://images.unsplash.com/photo-1743883325575-783014a39a8b?q=80&w=1287&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                                "detail", "auto"
                        )
                )))
        );

        long startTime = System.currentTimeMillis();
        var response = callOpenAI(
                objectMapper,
                openaiApiKey,
                "gpt-4o-mini",
                Map.of(),
                messages
        ).get();
        long endTime = System.currentTimeMillis();

        JsonNode bodyNode;
        try {
            bodyNode = objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse response body.", e);
        }

        JsonNode messageNode = bodyNode.get("choices").get(0).get("message");
        Object responseMessage = objectMapper.convertValue(messageNode, Object.class);
        messages.add(new ChatMessage(responseMessage));

        CallInfo callInfo = CallInfo.from(prompt.getPromptInfo(), startTime, endTime);
        ResponseInfo responseInfo = new ResponseInfo(true);

        fpClient.recordings().create(
                new RecordPayload(
                        projectId,
                        messages
                ).inputs(variables)
                        .promptVersionInfo(prompt.getPromptInfo())
                        .callInfo(callInfo)
                        .responseInfo(responseInfo)
        ).get();
    }
}
