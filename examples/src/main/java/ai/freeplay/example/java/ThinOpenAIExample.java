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

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.callOpenAIWithTools;
import static java.lang.String.format;

public class ThinOpenAIExample {
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

        Map<String, Object> variables = Map.of("location", "New York");

        fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        projectId,
                        "my-chat-template",
                        "latest",
                        variables,
                        null
                ).thenCompose((FormattedPrompt<List<ChatMessage>> formattedPrompt) -> {
                            long startTime = System.currentTimeMillis();
                            return callOpenAIWithTools(
                                    objectMapper,
                                    openaiApiKey,
                                    formattedPrompt.getPromptInfo().getModel(),
                                    formattedPrompt.getPromptInfo().getModelParameters(),
                                    formattedPrompt.getFormattedPrompt(),
                                    formattedPrompt.getToolSchema()
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
                                    .customMetadata(Map.of("custom_field", "custom_value"))
                                    .getSessionInfo();

                            System.out.println("Completion: " + messageNode.get("content").asText());

                            return fpClient.recordings().create(
                                    new RecordInfo(
                                            projectId,
                                            allMessages
                                    ).inputs(variables)
                                            .sessionInfo(sessionInfo)
                                            .promptVersionInfo(formattedPrompt.getPromptInfo())
                                            .callInfo(callInfo)
                                            .responseInfo(responseInfo)
                                            .toolSchema(formattedPrompt.getToolSchema()));
                        }
                )
                .exceptionally(exception -> {
                    System.out.println("Got exception: " + exception.getMessage());
                    return new RecordResponse(null);
                })
                .join();
    }
}
