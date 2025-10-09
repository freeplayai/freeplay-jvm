package ai.freeplay.client.integration;

import ai.freeplay.client.SlowTest;
import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.Prompts;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.lang.String.format;
import static org.junit.Assert.*;

public class BedrockConverseToolCallTest {
    String awsAccessKeyId = requireEnv("AWS_ACCESS_KEY_ID");
    String awsSecretAccessKey = requireEnv("AWS_SECRET_ACCESS_KEY");
    String freeplayApiKey = requireEnv("FREEPLAY_API_KEY");
    String projectId = requireEnv("FREEPLAY_PROJECT_ID");
    String freeplayUrl = requireEnv("FREEPLAY_API_URL");

    // Allow customizing template name via environment variable
    String toolCallTemplateName = System.getenv().getOrDefault("BEDROCK_TOOL_TEMPLATE", "nova_tool_call");

    String baseUrl = format("%s/api", freeplayUrl);

    Freeplay freeplay = new Freeplay(Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    );

    BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)
                    )
            )
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Category(SlowTest.class)
    public void testToolCall() throws IOException, ExecutionException, InterruptedException {
        String query = "What is 5 plus 3?";
        Map<String, Object> variables = Map.of("equation", query);

        var formatRequest = new Prompts.GetFormattedRequest(projectId, toolCallTemplateName, "latest", variables)
                .flavorName("bedrock_converse")
                .history(Collections.emptyList());

        FormattedPrompt<List<Map<String, Object>>> formattedPrompt = freeplay.prompts().<List<Map<String, Object>>>getFormatted(formatRequest).get();

        // Define a simple add tool
        Tool addTool = buildAddTool();

        Message userMessage = Message.builder()
                .role("user")
                .content(ContentBlock.fromText(query))
                .build();

        List<Message> conversationHistory = new ArrayList<>();
        conversationHistory.add(userMessage);

        // Make Bedrock Converse call with tool
        ConverseResponse response = bedrockClient.converse(
                ConverseRequest.builder()
                        .modelId(formattedPrompt.getPromptInfo().getModel())
                        .messages(conversationHistory)
                        .toolConfig(ToolConfiguration.builder()
                                .tools(addTool)
                                .build())
                        .inferenceConfig(InferenceConfiguration.builder()
                                .maxTokens(2000)
                                .build())
                        .build()
        );

        // Check if tool was called
        assertEquals(StopReason.TOOL_USE, response.stopReason());

        // Extract tool use
        Message assistantMessage = response.output().message();
        conversationHistory.add(assistantMessage);

        ToolUseBlock toolUse = assistantMessage.content().stream()
                .filter(block -> block.toolUse() != null)
                .map(ContentBlock::toolUse)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No tool use found"));

        assertEquals("add_numbers", toolUse.name());

        // Execute tool
        Map<String, Object> toolInput = objectMapper.readValue(
                toolUse.input().toString(),
                Map.class
        );

        @SuppressWarnings("unchecked")
        List<Integer> numbers = (List<Integer>) toolInput.get("numbers");
        int result = numbers.stream().mapToInt(Integer::intValue).sum();
        assertEquals(8, result);

        // Send tool result back
        Message toolResultMessage = Message.builder()
                .role("user")
                .content(ContentBlock.fromToolResult(
                        ToolResultBlock.builder()
                                .toolUseId(toolUse.toolUseId())
                                .content(ToolResultContentBlock.fromText(String.valueOf(result)))
                                .build()
                ))
                .build();

        conversationHistory.add(toolResultMessage);

        // Get final response
        ConverseResponse finalResponse = bedrockClient.converse(
                ConverseRequest.builder()
                        .modelId(formattedPrompt.getPromptInfo().getModel())
                        .messages(conversationHistory)
                        .toolConfig(ToolConfiguration.builder()
                                .tools(addTool)
                                .build())
                        .inferenceConfig(InferenceConfiguration.builder()
                                .maxTokens(2000)
                                .build())
                        .build()
        );

        String finalText = finalResponse.output().message().content().get(0).text();
        assertTrue("Expected response to contain '8'", finalText.contains("8"));

        // Record to Freeplay
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("user", query));
        chatMessages.add(new ChatMessage("assistant", "Calling tool: add_numbers"));
        chatMessages.add(new ChatMessage("user", "Tool result: " + result));
        chatMessages.add(new ChatMessage("assistant", finalText));

        RecordResponse recordResponse = freeplay.recordings().create(new RecordInfo(
                projectId,
                chatMessages)
                .inputs(variables)
                .promptVersionInfo(formattedPrompt.getPromptInfo())
                .callInfo(CallInfo.from(formattedPrompt.getPromptInfo(), System.currentTimeMillis() - 2_000, System.currentTimeMillis()))
                .responseInfo(new ResponseInfo(true))
        ).get();

        assertNotNull(recordResponse.getCompletionId());
    }

    private Tool buildAddTool() {
        // Build tool schema using Document API
        Map<String, Document> numbersProperty = new HashMap<>();
        numbersProperty.put("type", Document.fromString("array"));
        numbersProperty.put("items", Document.fromMap(Map.of("type", Document.fromString("integer"))));
        numbersProperty.put("description", Document.fromString("List of numbers to add"));

        Map<String, Document> properties = new HashMap<>();
        properties.put("numbers", Document.fromMap(numbersProperty));

        Map<String, Document> schema = new HashMap<>();
        schema.put("type", Document.fromString("object"));
        schema.put("properties", Document.fromMap(properties));
        schema.put("required", Document.fromList(List.of(Document.fromString("numbers"))));

        return Tool.builder()
                .toolSpec(ToolSpecification.builder()
                        .name("add_numbers")
                        .description("Add a list of numbers together")
                        .inputSchema(ToolInputSchema.builder()
                                .json(Document.fromMap(schema))
                                .build())
                        .build())
                .build();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null) {
            fail("Missing required environment variable: " + name);
        }
        return value;
    }
}
