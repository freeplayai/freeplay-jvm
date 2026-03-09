package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FormattedPrompt;
import ai.freeplay.client.resources.prompts.Prompts.GetFormattedRequest;
import ai.freeplay.client.resources.recordings.CallInfo;
import ai.freeplay.client.resources.recordings.RecordPayload;
import ai.freeplay.client.resources.recordings.ResponseInfo;
import ai.freeplay.client.resources.sessions.CreateTracePayload;
import ai.freeplay.client.resources.sessions.Session;
import ai.freeplay.client.resources.sessions.TraceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.example.java.ExampleUtils.callAnthropic;
import static java.lang.String.format;

public class ThinTrace {
    static String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
    static String projectId = System.getenv("FREEPLAY_PROJECT_ID");
    static String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
    static String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static Freeplay fpClient = new Freeplay(Config()
            .freeplayAPIKey(freeplayApiKey)
            .customerDomain(customerDomain)
    );

    public static String call(
            String projectId,
            String templateName,
            String environment,
            Map<String, Object> variables,
            TraceInfo traceInfo
    ) {
        return fpClient.prompts()
                .<List<ChatMessage>>getFormatted(
                        new GetFormattedRequest(projectId, templateName, environment, variables)
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
                            String output = bodyNode.path("content").get(0).path("text").asText();
                            System.out.println("Completion: " + output);

                            RecordPayload recordPayload = new RecordPayload(
                                    projectId,
                                    allMessages
                            ).inputs(variables)
                                    .promptVersionInfo(formattedPrompt.getPromptInfo())
                                    .callInfo(callInfo)
                                    .responseInfo(responseInfo)
                                    .parentId(traceInfo.getTraceId());

                            return fpClient.recordings()
                                    .create(recordPayload)
                                    .thenApply(ignored -> output);
                        }
                )
                .exceptionally(exception -> {
                    System.out.println("Got exception: " + exception.getMessage());
                    exception.printStackTrace();
                    return null;
                })
                .join();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String input = "What is the meaning of life?";
        Map<String, Object> inputVars = Map.of("question", input);

        Session session = fpClient.sessions().create();
        TraceInfo traceInfo = session.createTrace(new CreateTracePayload(input).agentName("agent_name").customMetadata(Map.of("some_custom_metadata", "hello")));

        String response = call(
                projectId,
                "my-anthropic-prompt",
                "latest",
                inputVars,
                traceInfo
        );
        System.out.println("First Completion: " + response);

        Map<String, Object> inputVars2 = Map.of("question", format("categorize the following question: %s", input));
        String category = call(
                projectId,
                "my-anthropic-prompt",
                "latest",
                inputVars2,
                traceInfo
        );
        traceInfo.recordOutput(projectId, response, Map.of("bool_field", true, "float_value", 0.2));
        System.out.println("Second Completion: " + category);

        System.out.println("Recorded Trace " + traceInfo.traceId + " to session " + traceInfo.sessionId + " with input " + traceInfo.input + " and output " + response);

        fpClient.customerFeedback().updateTrace(projectId, String.valueOf(traceInfo.traceId), Map.of("freeplay_feedback", "positive")).get();
    }
}
