package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ai.freeplay.client.thin.Freeplay.Config;

public class StartFromRecordExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // Setup clients
        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(System.getenv("FREEPLAY_API_KEY"))
                .baseUrl(System.getenv("FREEPLAY_API_URL") + "/api")
        );
        
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String name = "John";

        // Create messages
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", 
            "You just say good job when someone tells their name. Like 'Good job, <name>!'"));
        messages.add(new ChatMessage("user", "My name is " + name + "."));

        // Call OpenAI
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", "gpt-3.5-turbo",
                "messages", messages
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openaiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode responseJson = objectMapper.readTree(response.body());
        String assistantMessage = responseJson.path("choices").get(0)
                .path("message").path("content").asText();

        // Add assistant response to messages
        messages.add(new ChatMessage("assistant", assistantMessage));

        // Record to Freeplay (with auto-generated session ID)
        RecordResponse recordResponse = fpClient.recordings().create(
                new RecordInfo(projectId, messages)
        ).get();

        System.out.println("Recorded with completion ID: " + recordResponse.getCompletionId());
        System.out.println("Assistant said: " + assistantMessage);
    }
}