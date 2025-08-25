package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.recordings.*;
import ai.freeplay.client.thin.resources.sessions.SessionInfo;
import ai.freeplay.example.java.ThinExampleUtils.OpenAIFunctionCallDTO;
import ai.freeplay.example.java.ThinExampleUtils.OpenAIFunctionCallDTO.Parameters;
import ai.freeplay.example.java.ThinExampleUtils.Tuple3;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static ai.freeplay.example.java.ThinExampleUtils.OpenAIFunctionCallDTO.Property;
import static ai.freeplay.example.java.ThinExampleUtils.callOpenAI;

public class ThinFunctionCallExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws ExecutionException, InterruptedException, JsonProcessingException {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
        String openAIApiKey = System.getenv("OPENAI_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .customerDomain(customerDomain)
        );

        Map<String, Object> variables = Map.of("pop_star", "Bruno Mars");

        OpenAIFunctionCallDTO functionDefinition = new OpenAIFunctionCallDTO(
                "get_album_tracklist",
                "Given an album name and genre, return a list of songs.",
                new Parameters(
                        "object",
                        Map.of(
                                "album_name", new Property("string", "Name of album from which to retrieve tracklist."),
                                "genre", new Property("string", "Album genre")
                        )
                )
        );

        fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        projectId,
                        "album_bot",
                        "latest",
                        variables
                ).thenCompose((FormattedPrompt<List<ChatMessage>> formattedPrompt) -> {
                            long startTime = System.currentTimeMillis();
                            return callOpenAI(
                                    objectMapper,
                                    openAIApiKey,
                                    formattedPrompt.getPromptInfo().getModel(),
                                    formattedPrompt.getPromptInfo().getModelParameters(),
                                    formattedPrompt.getFormattedPrompt(),
                                    List.of(functionDefinition)
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
                            JsonNode functionNode = bodyNode.path("choices").get(0).path("message").path("function_call");
                            String functionName = functionNode.path("name").asText();
                            String functionArgs = functionNode.path("arguments").asText();
                            if (functionName == null || functionName.isEmpty()) {
                                functionName = "defaultFunctionName"; // Set to a default or predefined name
                            }

                            if (functionArgs == null || functionArgs.isEmpty()) {
                                functionArgs = "{'key': 'value'}"; // Set to default or predefined arguments
                            }

                            CallInfo callInfo = CallInfo.from(
                                    formattedPrompt.getPromptInfo(),
                                    startTime,
                                    System.currentTimeMillis()
                            );
                            ResponseInfo responseInfo = new ResponseInfo(
                                    "stop".equals(bodyNode.path("finish_reason").asText())
                            ).functionCall(new OpenAIFunctionCall(functionName, functionArgs));

                            System.out.printf("Function call: %s(%s)%n", functionName, functionArgs);

                            return fpClient.recordings().create(
                                    new RecordInfo(
                                            projectId,
                                            formattedPrompt.getBoundMessages()
                                    ).inputs(variables)
                                            .promptVersionInfo(formattedPrompt.getPromptInfo())
                                            .callInfo(callInfo)
                                            .responseInfo(responseInfo));
                        }
                )
                .exceptionally(exception -> {
                    System.out.println("Got exception: " + exception.getMessage());
                    return new RecordResponse(null);
                })
                .join();
    }
}
