package ai.freeplay.client.thin;

import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.ContentPartBase64;
import ai.freeplay.client.thin.resources.prompts.ContentPartText;
import ai.freeplay.client.thin.resources.prompts.MediaType;
import com.google.cloud.vertexai.api.Blob;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.protobuf.ByteString;
import org.junit.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GeminiLLMAdapterTest {
    @Test
    public void testToLLMSyntax() {
        LLMAdapters.LLMAdapter<?> adapter = LLMAdapters.adapterForFlavor("gemini_chat");
        byte[] audioBytes = Base64.getEncoder().encode("some audio data".getBytes());
        byte[] documentBytes = Base64.getEncoder().encode("some pdf data".getBytes());
        byte[] imageBytes = Base64.getEncoder().encode("some image data".getBytes());

        Object formattedMessages = adapter.toLLMSyntax(List.of(
                new ChatMessage("assistant", "Respond to the user's query"),
                new ChatMessage("user", List.of(
                        new ContentPartText("Some query"),
                        new ContentPartBase64("some-audio", MediaType.AUDIO, "audio/mpeg", audioBytes)
                )),
                new ChatMessage("user", List.of(
                        new ContentPartText("Some other query"),
                        new ContentPartBase64("some-file", MediaType.FILE, "application/pdf", documentBytes),
                        new ContentPartBase64("some-image-base64", MediaType.IMAGE, "image/png", imageBytes)
                ))

        ));

        assertEquals(List.of(
                        ContentMaker.forRole("model").fromString("Respond to the user's query"),
                        ContentMaker.forRole("user").fromMultiModalData(
                                Part.newBuilder().setText("Some query").build(),
                                Part.newBuilder().setInlineData(Blob.newBuilder().setMimeType("audio/mpeg").setData(ByteString.copyFrom("some audio data".getBytes()))).build()

                        ),
                        ContentMaker.forRole("user").fromMultiModalData(
                                Part.newBuilder().setText("Some other query").build(),
                                Part.newBuilder().setInlineData(Blob.newBuilder().setMimeType("application/pdf").setData(ByteString.copyFrom("some pdf data".getBytes()))).build(),
                                Part.newBuilder().setInlineData(Blob.newBuilder().setMimeType("image/png").setData(ByteString.copyFrom("some image data".getBytes()))).build()

                        )
                ),
                formattedMessages
        );
    }
}
