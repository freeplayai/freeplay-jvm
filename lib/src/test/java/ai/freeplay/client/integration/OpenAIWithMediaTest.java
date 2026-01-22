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
import ai.freeplay.client.thin.resources.sessions.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.lang.String.format;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static org.junit.Assert.*;

public class OpenAIWithMediaTest {
    String openAIApiKey = requireEnv("OPENAI_API_KEY");
    String freeplayApiKey = requireEnv("FREEPLAY_API_KEY");
    String projectId = requireEnv("FREEPLAY_PROJECT_ID");
    String freeplayUrl = requireEnv("FREEPLAY_API_URL");

    String baseUrl = format("%s/api", freeplayUrl);

    Freeplay freeplay = new Freeplay(Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Category(SlowTest.class)
    public void testWithImage() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        Map<String, Object> variables = Map.of("query", "Describe what you see");
        MediaInputCollection media = new MediaInputCollection();
        byte[] base64bytes = getBytes("media/whale.jpg");
        media.put("some-image", new MediaInputBase64(base64bytes, "image/jpeg"));

        var formatRequest = new Prompts.GetFormattedRequest(projectId, "media-image", "latest", variables)
                .flavorName("openai_chat")
                .mediaInputs(media);

        FormattedPrompt<List<ChatMessage>> formattedPrompt = freeplay.prompts().<List<ChatMessage>>getFormatted(formatRequest).get();

        var response = fetchCompletionResponse(formattedPrompt, "gpt-4o");

        assertEquals(200, response.statusCode());
        String responseContent = objectMapper.readTree(response.body()).get("choices").get(0).get("message").get("content").asText();

        assertTrue(String.format("Expected '%s' to contain whale", responseContent), responseContent.contains("whale"));

        RecordResponse recordResponse = fetchRecordResponse(formattedPrompt, responseContent, variables, media);
        assertNotNull(recordResponse.getCompletionId());
    }

    @Test
    @Category(SlowTest.class)
    public void testWithAudio() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        Map<String, Object> variables = Map.of("query", "Describe what you hear");
        MediaInputCollection media = new MediaInputCollection();
        byte[] base64bytes = getBytes("media/birds.mp3");
        media.put("some-audio", new MediaInputBase64(base64bytes, "audio/mpeg"));

        var formatRequest = new Prompts.GetFormattedRequest(projectId, "media-audio", "latest", variables)
                .flavorName("openai_chat")
                .mediaInputs(media);

        FormattedPrompt<List<ChatMessage>> formattedPrompt = freeplay.prompts().<List<ChatMessage>>getFormatted(formatRequest).get();

        var response = fetchCompletionResponse(formattedPrompt, "gpt-4o-audio-preview");

        assertEquals(200, response.statusCode());
        String responseContent = objectMapper.readTree(response.body()).get("choices").get(0).get("message").get("content").asText();

        assertTrue(String.format("Expected '%s' to contain bird", responseContent), responseContent.contains("bird"));

        RecordResponse recordResponse = fetchRecordResponse(formattedPrompt, responseContent, variables, media);
        assertNotNull(recordResponse.getCompletionId());
    }

    @Test
    @Category(SlowTest.class)
    public void testWithDocument() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        Map<String, Object> variables = Map.of("query", "Describe this document");
        MediaInputCollection media = new MediaInputCollection();
        byte[] base64bytes = getBytes("media/portugal.pdf");
        media.put("some-file", new MediaInputBase64(base64bytes, "application/pdf"));

        var formatRequest = new Prompts.GetFormattedRequest(projectId, "media-file", "latest", variables)
                .flavorName("openai_chat")
                .mediaInputs(media);

        FormattedPrompt<List<ChatMessage>> formattedPrompt = freeplay.prompts().<List<ChatMessage>>getFormatted(formatRequest).get();

        var response = fetchCompletionResponse(formattedPrompt, "gpt-4o");

        assertEquals(200, response.statusCode());
        String responseContent = objectMapper.readTree(response.body()).get("choices").get(0).get("message").get("content").asText();

        assertTrue(String.format("Expected '%s' to contain Portugal", responseContent), responseContent.contains("Portugal"));

        RecordResponse recordResponse = fetchRecordResponse(formattedPrompt, responseContent, variables, media);
        assertNotNull(recordResponse.getCompletionId());
    }

    private byte[] getBytes(String filePath) throws URISyntaxException, IOException {
        File imageFile = new File(this.getClass().getClassLoader().getResource(filePath).toURI());
        return Base64.getEncoder().encode(Files.readAllBytes(imageFile.toPath()));
    }

    private RecordResponse fetchRecordResponse(FormattedPrompt<List<ChatMessage>> formattedPrompt, String responseContent, Map<String, Object> variables, MediaInputCollection media) throws InterruptedException, ExecutionException {
        List<ChatMessage> sentMessages = new ArrayList<>(formattedPrompt.getFormattedPrompt());
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

    private HttpResponse<String> fetchCompletionResponse(FormattedPrompt<List<ChatMessage>> formattedPrompt, String model) throws URISyntaxException, IOException, InterruptedException {
        var requestBuilder = HttpRequest
                .newBuilder(new URI("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", format("Bearer %s", openAIApiKey))
                .POST(ofString(objectMapper.writeValueAsString(
                        Map.of(
                                "model", model,
                                "messages", formattedPrompt.getFormattedPrompt()
                        )
                )));

        return HttpClient.newBuilder()
                .build().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null) {
            fail("Missing required environment variable: " + name);
        }
        return value;
    }
}
