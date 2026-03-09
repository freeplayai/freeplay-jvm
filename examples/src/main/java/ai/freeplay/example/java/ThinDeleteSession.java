package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FormattedPrompt;
import ai.freeplay.client.resources.prompts.Prompts.GetFormattedRequest;
import ai.freeplay.client.resources.recordings.CallInfo;
import ai.freeplay.client.resources.recordings.RecordPayload;
import ai.freeplay.client.resources.recordings.ResponseInfo;
import ai.freeplay.client.resources.sessions.SessionDeleteResponse;
import ai.freeplay.client.resources.sessions.SessionInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.example.java.ExampleUtils.callAnthropic;

public class ThinDeleteSession {
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

        fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        new GetFormattedRequest(projectId, "my-anthropic-prompt", "latest", variables)
                ).thenCompose((FormattedPrompt<List<ChatMessage>> formattedPrompt) -> {
                            long startTime = System.currentTimeMillis();
                            return callAnthropic(
                                    objectMapper,
                                    anthropicApiKey,
                                    formattedPrompt.getPromptInfo().getModel(),
                                    formattedPrompt.getPromptInfo().getModelParameters(),
                                    formattedPrompt.getFormattedPrompt(),
                                    formattedPrompt.getSystemContent().orElse(null)
                            ).thenApply((HttpResponse<String> response) ->
                                    new ExampleUtils.Tuple3<>(formattedPrompt, response, startTime)
                            );
                        }
                ).thenCompose((ExampleUtils.Tuple3<FormattedPrompt<List<ChatMessage>>, HttpResponse<String>, Long> promptAndResponse) -> {
                            FormattedPrompt<List<ChatMessage>> formattedPrompt = promptAndResponse.first;
                            HttpResponse<String> response = promptAndResponse.second;
                            long startTime = promptAndResponse.third;

                            JsonNode bodyNode;
                            try {
                                bodyNode = objectMapper.readTree(response.body());
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException("Unable to parse response body.", e);
                            }

                            List<ChatMessage> allMessages = formattedPrompt.allMessages(
                                    new ChatMessage("assistant", bodyNode.path("content").get(0).path("text").asText())
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
                                    .getSessionInfo();

                            System.out.println("Creating Session with ID: " + sessionInfo.getSessionId());

                            System.out.println("Completion: " + bodyNode.path("content").get(0).path("text").asText());

                            return fpClient.recordings().create(
                                    new RecordPayload(
                                            projectId,
                                            allMessages
                                    ).inputs(variables)
                                            .sessionInfo(sessionInfo)
                                            .promptVersionInfo(formattedPrompt.getPromptInfo())
                                            .callInfo(callInfo)
                                            .responseInfo(responseInfo)
                            ).thenCompose(ignored -> fpClient.sessions().delete(projectId, sessionInfo.getSessionId()));
                        }
                ).thenApply((SessionDeleteResponse deleteResponse) -> {
                    System.out.printf("Delete response: %s%n", deleteResponse);
                    return deleteResponse;
                })
                .exceptionally(exception -> {
                    System.out.println("Got exception: " + exception.getMessage());
                    exception.printStackTrace();
                    return new SessionDeleteResponse();
                })
                .join();
    }
}
