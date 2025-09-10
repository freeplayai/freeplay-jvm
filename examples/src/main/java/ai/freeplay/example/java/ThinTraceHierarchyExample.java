package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.recordings.*;
import ai.freeplay.client.thin.resources.sessions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.example.java.ThinExampleUtils.callAnthropic;

import static ai.freeplay.client.thin.Freeplay.Config;

/**
 * Java equivalent of the Python trace hierarchy example.
 * Demonstrates parent-child relationships between traces and completions using the new parentId parameter.
 */
public class ThinTraceHierarchyExample {
    private static final String FREEPLAY_API_KEY = System.getenv("FREEPLAY_API_KEY");
    private static final String FREEPLAY_API_URL = System.getenv("FREEPLAY_API_URL");
    private static final String PROJECT_ID = System.getenv("FREEPLAY_PROJECT_ID");
    private static final String ANTHROPIC_API_KEY = System.getenv("ANTHROPIC_API_KEY");

    private static final Random random = new Random();

    public static void main(String[] args) {
        if (FREEPLAY_API_KEY == null || PROJECT_ID == null || ANTHROPIC_API_KEY == null) {
            throw new RuntimeException("Missing required environment variables: FREEPLAY_API_KEY, FREEPLAY_PROJECT_ID, ANTHROPIC_API_KEY");
        }

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(FREEPLAY_API_KEY)
                .baseUrl(FREEPLAY_API_URL + "/api"));

        try {
            runTraceHierarchyExample(fpClient);
        } catch (Exception e) {
            System.err.println("Error in trace hierarchy example: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTraceHierarchyExample(Freeplay fpClient) throws Exception {
        // Create session
        Session session = fpClient.sessions().create()
                .customMetadata(Map.of("metadata_123", "blah"));

        String[] userQuestions = {
            "answer life's most existential questions", 
            "what is sand?", 
            "how tall are lions?"
        };

        UUID lastTraceId = null;

        for (String question : userQuestions) {
            System.out.println("\n=== Processing question: " + question + " ===");

            // Create trace with parent relationship (chaining traces together)
            TraceInfo traceInfo = session.createTrace(question)
                    .agentName("mr-secret-agent")
                    .customMetadata(Map.of("metadata_key", "hello"))
                    .parentId(lastTraceId);  // Using new parentId parameter

            System.out.println("Created trace: " + traceInfo.getTraceId() + 
                (traceInfo.getParentId() != null ? " (parent: " + traceInfo.getParentId() + ")" : " (root)"));

            // First LLM call - answer the question
            CallAndRecordResult botResponse = callAndRecord(
                fpClient,
                PROJECT_ID,
                "my-anthropic-prompt",
                "latest",
                Map.of("question", question),
                session.getSessionInfo(),
                lastTraceId != null ? lastTraceId : traceInfo.getTraceId()
            );

            // Second LLM call - categorize the question (child of first completion)
            CallAndRecordResult categorizationResult = callAndRecord(
                fpClient,
                PROJECT_ID,
                "question-classifier", 
                "latest",
                Map.of("question", question),
                session.getSessionInfo(),
                UUID.fromString(botResponse.completionId)  // Parent is the first completion
            );

            // Send customer feedback for the completion
            System.out.println("Sending customer feedback for completion id: " + botResponse.completionId);
            fpClient.customerFeedback().update(
                PROJECT_ID,
                botResponse.completionId,
                Map.of(
                    "is_it_good", random.nextBoolean() ? "yuh" : "nah",
                    "topic", categorizationResult.llmResponse
                )
            ).get();

            // Record trace output with eval results
            traceInfo.recordOutput(
                PROJECT_ID,
                botResponse.llmResponse,
                Map.of("bool_field", false, "num_field", 0.9)
            ).get();

            // Record feedback for the trace
            Map<String, Object> traceFeedback = Map.of(
                "is_it_good", random.nextBoolean(),
                "freeplay_feedback", random.nextBoolean() ? "positive" : "negative"
            );
            fpClient.customerFeedback().updateTrace(PROJECT_ID, traceInfo.getTraceId().toString(), traceFeedback).get();

            System.out.println("Trace info id: " + traceInfo.getTraceId());
            lastTraceId = traceInfo.getTraceId();
        }

        System.out.println("\n=== Trace Hierarchy Example Complete ===");
        System.out.println("Created " + userQuestions.length + " chained traces in session: " + session.getSessionId());
        System.out.println("Each trace (except the first) has a parent relationship using parentId parameter.");
    }

    private static CallAndRecordResult callAndRecord(
            Freeplay fpClient,
            String projectId,
            String templateName,
            String env,
            Map<String, Object> inputVariables,
            SessionInfo sessionInfo,
            UUID parentId
    ) throws Exception {
        // Get formatted prompt
        FormattedPrompt<Object> formattedPrompt = fpClient.prompts().getFormatted(
                projectId,
                templateName,
                env,
                inputVariables,
                "anthropic_chat"
        ).get();

        System.out.println("Ready for LLM: " + formattedPrompt.getFormattedPrompt());

        // Call Anthropic API
        long start = System.currentTimeMillis();
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        List<ChatMessage> messages = (List<ChatMessage>) formattedPrompt.getFormattedPrompt();
        CompletableFuture<HttpResponse<String>> responseFuture = callAnthropic(
                objectMapper,
                ANTHROPIC_API_KEY,
                formattedPrompt.getPromptInfo().getModel(),
                formattedPrompt.getPromptInfo().getModelParameters(),
                messages,
                formattedPrompt.getSystemContent().orElse(null),
                formattedPrompt.getToolSchema()
        );
        
        HttpResponse<String> response = responseFuture.get();
        long end = System.currentTimeMillis();
        
        // Parse Anthropic response
        JsonNode bodyNode = objectMapper.readTree(response.body());
        String llmResponse = bodyNode.path("content").get(0).path("text").asText();

        System.out.println("Completion: " + llmResponse);

        // Prepare recording data
        List<Object> content = objectMapper.convertValue(bodyNode.get("content"), List.class);
        List<ChatMessage> allMessages = formattedPrompt.allMessages(
                new ChatMessage("assistant", content)
        );
        CallInfo callInfo = CallInfo.from(formattedPrompt.getPromptInfo(), start, end)
                .usage(new CallInfo.UsageTokens(
                        bodyNode.get("usage").get("input_tokens").asInt(),
                        bodyNode.get("usage").get("output_tokens").asInt()
                ));
        ResponseInfo responseInfo = new ResponseInfo(
                "stop_sequence".equals(bodyNode.path("stop_reason").asText())
        );

        // Create RecordInfo with parentId (new approach)
        RecordInfo recordInfo = new RecordInfo(projectId, allMessages)
                .sessionInfo(sessionInfo)
                .inputs(inputVariables)
                .promptVersionInfo(formattedPrompt.getPromptInfo())
                .callInfo(callInfo)
                .responseInfo(responseInfo)
                .parentId(parentId);  // Using new parentId instead of deprecated traceInfo

        // Print RecordInfo details before recording
        System.out.println("RecordInfo details:");
        System.out.println("  Project ID: " + recordInfo.getProjectId());
        System.out.println("  Messages count: " + (recordInfo.getAllMessages() != null ? recordInfo.getAllMessages().size() : "null"));
        System.out.println("  Session ID: " + (recordInfo.getSessionInfo() != null ? recordInfo.getSessionInfo().getSessionId() : "null"));
        System.out.println("  Parent ID: " + recordInfo.getParentId());
        System.out.println("  Inputs: " + recordInfo.getInputs());
        System.out.println("  Prompt Version: " + (recordInfo.getPromptVersionInfo() != null ? recordInfo.getPromptVersionInfo().getPromptTemplateVersionId() : "null"));
        System.out.println("  Call Info: " + (recordInfo.getCallInfo() != null ? "present" : "null"));
        System.out.println("  Response Info: " + (recordInfo.getResponseInfo() != null ? "present" : "null"));

        RecordResponse recordResponse = fpClient.recordings().create(recordInfo).get();

        return new CallAndRecordResult(recordResponse.getCompletionId(), llmResponse);
    }


    private static class CallAndRecordResult {
        final String completionId;
        final String llmResponse;

        CallAndRecordResult(String completionId, String llmResponse) {
            this.completionId = completionId;
            this.llmResponse = llmResponse;
        }
    }
}
