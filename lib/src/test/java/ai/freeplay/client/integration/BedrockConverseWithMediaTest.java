package ai.freeplay.client.integration;

import ai.freeplay.client.SlowTest;
import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputCollection;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.lang.String.format;
import static org.junit.Assert.*;

public class BedrockConverseWithMediaTest {
    String awsAccessKeyId = requireEnv("AWS_ACCESS_KEY_ID");
    String awsSecretAccessKey = requireEnv("AWS_SECRET_ACCESS_KEY");
    String freeplayApiKey = requireEnv("FREEPLAY_API_KEY");
    String projectId = requireEnv("FREEPLAY_PROJECT_ID");
    String freeplayUrl = requireEnv("FREEPLAY_API_URL");

    // Allow customizing template names via environment variables
    String imageTemplateName = System.getenv().getOrDefault("BEDROCK_IMAGE_TEMPLATE", "nova_image_test");
    String fileTemplateName = System.getenv().getOrDefault("BEDROCK_FILE_TEMPLATE", "documenter");

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
    public void testWithImage() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        Map<String, Object> variables = Map.of("query", "Describe what you see");
        MediaInputCollection media = new MediaInputCollection();
        File imageFile = new File(this.getClass().getClassLoader().getResource("media/whale.jpg").toURI());
        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
        // MediaInputBase64 expects base64-encoded bytes
        byte[] base64EncodedBytes = Base64.getEncoder().encode(imageBytes);
        media.put("city-image", new MediaInputBase64(base64EncodedBytes, "image/jpeg"));

        var formatRequest = new Prompts.GetFormattedRequest(projectId, imageTemplateName, "latest", variables)
                .flavorName("bedrock_converse")
                .mediaInputs(media);

        FormattedPrompt<List<Map<String, Object>>> formattedPrompt = freeplay.prompts().<List<Map<String, Object>>>getFormatted(formatRequest).get();

        // Build Bedrock message with image
        List<ContentBlock> contentBlocks = new ArrayList<>();
        contentBlocks.add(ContentBlock.fromImage(
                ImageBlock.builder()
                        .format("jpeg")
                        .source(ImageSource.builder()
                                .bytes(SdkBytes.fromByteArray(imageBytes))
                                .build())
                        .build()
        ));
        contentBlocks.add(ContentBlock.fromText(variables.get("query").toString()));

        Message userMessage = Message.builder()
                .role("user")
                .content(contentBlocks)
                .build();

        // Make Bedrock Converse call
        ConverseResponse response = bedrockClient.converse(
                ConverseRequest.builder()
                        .modelId(formattedPrompt.getPromptInfo().getModel())
                        .messages(Collections.singletonList(userMessage))
                        .inferenceConfig(InferenceConfiguration.builder()
                                .maxTokens(2000)
                                .build())
                        .build()
        );

        String responseContent = response.output().message().content().get(0).text();
        assertTrue(String.format("Expected '%s' to contain whale", responseContent),
                responseContent.toLowerCase().contains("whale"));

        // Record to Freeplay
        RecordResponse recordResponse = fetchRecordResponse(formattedPrompt, responseContent, variables, media);
        assertNotNull(recordResponse.getCompletionId());
    }

    @Test
    @Category(SlowTest.class)
    public void testWithDocument() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        Map<String, Object> variables = Map.of("query", "Describe this document");
        MediaInputCollection media = new MediaInputCollection();
        File pdfFile = new File(this.getClass().getClassLoader().getResource("media/portugal.pdf").toURI());
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
        // MediaInputBase64 expects base64-encoded bytes
        byte[] base64EncodedBytes = Base64.getEncoder().encode(pdfBytes);
        media.put("document", new MediaInputBase64(base64EncodedBytes, "application/pdf"));

        var formatRequest = new Prompts.GetFormattedRequest(projectId, fileTemplateName, "latest", variables)
                .flavorName("bedrock_converse")
                .mediaInputs(media);

        FormattedPrompt<List<Map<String, Object>>> formattedPrompt = freeplay.prompts().<List<Map<String, Object>>>getFormatted(formatRequest).get();

        // Build Bedrock message with document
        List<ContentBlock> contentBlocks = new ArrayList<>();
        contentBlocks.add(ContentBlock.fromDocument(
                DocumentBlock.builder()
                        .format("pdf")
                        .name("portugal")
                        .source(DocumentSource.builder()
                                .bytes(SdkBytes.fromByteArray(pdfBytes))
                                .build())
                        .build()
        ));
        contentBlocks.add(ContentBlock.fromText(variables.get("query").toString()));

        Message userMessage = Message.builder()
                .role("user")
                .content(contentBlocks)
                .build();

        // Make Bedrock Converse call
        ConverseResponse response = bedrockClient.converse(
                ConverseRequest.builder()
                        .modelId(formattedPrompt.getPromptInfo().getModel())
                        .messages(Collections.singletonList(userMessage))
                        .inferenceConfig(InferenceConfiguration.builder()
                                .maxTokens(2000)
                                .build())
                        .build()
        );

        String responseContent = response.output().message().content().get(0).text();
        assertTrue(String.format("Expected '%s' to contain Portugal", responseContent),
                responseContent.toLowerCase().contains("portugal"));

        // Record to Freeplay
        RecordResponse recordResponse = fetchRecordResponse(formattedPrompt, responseContent, variables, media);
        assertNotNull(recordResponse.getCompletionId());
    }

    private RecordResponse fetchRecordResponse(
            FormattedPrompt<List<Map<String, Object>>> formattedPrompt,
            String responseContent,
            Map<String, Object> variables,
            MediaInputCollection media
    ) throws InterruptedException, ExecutionException {
        List<ChatMessage> sentMessages = new ArrayList<>();
        // Add user message
        sentMessages.add(new ChatMessage("user", variables.get("query").toString()));
        // Add assistant response
        sentMessages.add(new ChatMessage("assistant", responseContent));

        return freeplay.recordings().create(new RecordInfo(
                projectId,
                sentMessages)
                .inputs(variables)
                .promptVersionInfo(formattedPrompt.getPromptInfo())
                .callInfo(CallInfo.from(formattedPrompt.getPromptInfo(), System.currentTimeMillis() - 1_000, System.currentTimeMillis()))
                .responseInfo(new ResponseInfo(true))
                .mediaInputCollection(media)
        ).get();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null) {
            fail("Missing required environment variable: " + name);
        }
        return value;
    }
}
