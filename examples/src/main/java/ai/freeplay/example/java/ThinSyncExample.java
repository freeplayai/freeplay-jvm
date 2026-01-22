package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.sessions.SessionInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.callAnthropic;

public class ThinSyncExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .customerDomain(customerDomain)
        );

        Map<String, Object> variables = Map.of("question", "Why isn't my window working?");

        FormattedPrompt<List<ChatMessage>> prompt = fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        projectId,
                        "my-prompt-anthropic",
                        "prod",
                        variables
                ).get();

        long startTime = System.currentTimeMillis();
        HttpResponse<String> llmResponse = callAnthropic(
                objectMapper,
                anthropicApiKey,
                prompt.getPromptInfo().getModel(),
                prompt.getPromptInfo().getModelParameters(),
                prompt.getFormattedPrompt(),
                prompt.getSystemContent().orElse(null)
        ).get();

        RecordResponse recordResponse = recordResult(
                fpClient,
                projectId,
                prompt,
                variables,
                startTime,
                llmResponse
        ).get();
        System.out.println("Recorded with completionId: " + recordResponse.getCompletionId());
    }

    public static CompletableFuture<RecordResponse> recordResult(
            Freeplay fpClient,
            String projectId,
            FormattedPrompt<List<ChatMessage>> formattedPrompt, Map<String, Object> variables, long startTime, HttpResponse<String> response
    ) {
        JsonNode bodyNode;
        try {
            bodyNode = objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse response body.", e);
        }

        List<ChatMessage> allMessages = formattedPrompt.allMessages(
                new ChatMessage("Assistant", bodyNode.path("completion").asText())
        );

        CallInfo callInfo = CallInfo.from(
                formattedPrompt.getPromptInfo(),
                startTime,
                System.currentTimeMillis()
        );
        ResponseInfo responseInfo = new ResponseInfo(
                "stop_sequence".equals(bodyNode.path("stop_reason").asText())
        );
        SessionInfo sessionInfo = fpClient.sessions().create()
                .customMetadata(Map.of("custom_field", "custom_value"))
                .getSessionInfo();

        System.out.println("Completion: " + bodyNode.path("completion").asText());

        return fpClient.recordings().create(
                new RecordInfo(
                        projectId,
                        allMessages
                ).inputs(variables)
                        .sessionInfo(sessionInfo)
                        .promptVersionInfo(formattedPrompt.getPromptInfo())
                        .callInfo(callInfo)
                        .responseInfo(responseInfo));
    }
}
