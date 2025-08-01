package ai.freeplay.client.integration;

import ai.freeplay.client.SlowTest;
import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.Prompts;
import ai.freeplay.client.thin.resources.recordings.CallInfo;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import ai.freeplay.client.thin.resources.recordings.RecordResponse;
import ai.freeplay.client.thin.resources.recordings.ResponseInfo;
import ai.freeplay.client.thin.resources.sessions.Session;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.lang.String.format;
import static org.junit.Assert.*;

public class GeminiWithMediaTest {
    String freeplayApiKey = requireEnv("FREEPLAY_API_KEY");
    String projectId = requireEnv("FREEPLAY_PROJECT_ID");
    String geminiProjectId = requireEnv("GEMINI_PROJECT_ID");
    String freeplayUrl = requireEnv("FREEPLAY_API_URL");

    String baseUrl = format("%s/api", freeplayUrl);

    Freeplay freeplay = new Freeplay(Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    );

    @Test
    @Category(SlowTest.class)
    public void testWithImage() throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        Map<String, Object> variables = Map.of("query", "Describe what you see");
        MediaInputCollection media = new MediaInputCollection();
        byte[] base64bytes = getBytes("media/whale.jpg");
        media.put("some-image", new MediaInputBase64(base64bytes, "image/jpeg"));

        var formatRequest = new Prompts.GetFormattedRequest(projectId, "media-image", "latest", variables)
                .flavorName("gemini_chat")
                .mediaInputs(media);
        FormattedPrompt<List<Content>> formattedPrompt = freeplay.prompts().<List<Content>>getFormatted(formatRequest).get();

        var response = fetchCompletionResponse(formattedPrompt.getFormattedPrompt());

        String responseContent = response.getCandidates(0).getContent().getParts(0).getText();
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
                .flavorName("gemini_chat")
                .mediaInputs(media);
        FormattedPrompt<List<Content>> formattedPrompt = freeplay.prompts().<List<Content>>getFormatted(formatRequest).get();

        var response = fetchCompletionResponse(formattedPrompt.getFormattedPrompt());

        String responseContent = response.getCandidates(0).getContent().getParts(0).getText();
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
                .flavorName("gemini_chat")
                .mediaInputs(media);
        FormattedPrompt<List<Content>> formattedPrompt = freeplay.prompts().<List<Content>>getFormatted(formatRequest).get();

        var response = fetchCompletionResponse(formattedPrompt.getFormattedPrompt());

        String responseContent = response.getCandidates(0).getContent().getParts(0).getText();
        assertTrue(String.format("Expected '%s' to contain Portugal", responseContent), responseContent.contains("Portugal"));

        RecordResponse recordResponse = fetchRecordResponse(formattedPrompt, responseContent, variables, media);
        assertNotNull(recordResponse.getCompletionId());
    }

    private byte[] getBytes(String filePath) throws URISyntaxException, IOException {
        File imageFile = new File(this.getClass().getClassLoader().getResource(filePath).toURI());
        return Base64.getEncoder().encode(Files.readAllBytes(imageFile.toPath()));
    }

    private RecordResponse fetchRecordResponse(FormattedPrompt<List<Content>> formattedPrompt, String responseContent, Map<String, Object> variables, MediaInputCollection media) throws InterruptedException, ExecutionException {
        List<Content> sentMessages = new ArrayList<>(formattedPrompt.getFormattedPrompt());
        sentMessages.add(ContentMaker.forRole("model").fromString(responseContent));

        Session session = freeplay.sessions().create();
        return freeplay.recordings().create(RecordInfo.fromGeminiContent(
                projectId,
                sentMessages,
                variables,
                session.getSessionInfo(),
                formattedPrompt.getPromptInfo(),
                CallInfo.from(formattedPrompt.getPromptInfo(), System.currentTimeMillis() - 1_000, System.currentTimeMillis()),
                new ResponseInfo(true))
                .mediaInputCollection(media)
        ).get();
    }

    private GenerateContentResponse fetchCompletionResponse(List<Content> content) throws IOException {
        try (VertexAI vertexAi = new VertexAI(geminiProjectId, "us-central1"); ) {
            GenerativeModel model = new GenerativeModel("gemini-2.0-flash-001", vertexAi);
            return model.generateContent(content);
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null) {
            fail("Missing required environment variable: " + name);
        }
        return value;
    }
}
