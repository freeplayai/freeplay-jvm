package ai.freeplay.client.thin;

import ai.freeplay.client.thin.resources.prompts.*;
import org.junit.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class OpenAILLMAdapterTest {
    @Test
    public void testToLLMSyntax() {
        LLMAdapters.LLMAdapter<?> adapter = LLMAdapters.adapterForFlavor("openai_chat");

        Object formattedMessages = adapter.toLLMSyntax(List.of(
                new ChatMessage("system", "Respond to the user's query"),
                new ChatMessage("user", List.of(
                        new ContentPartText("Some query"),
                        new ContentPartUrl("some-image", MediaType.IMAGE, "http://localhost/image"),
                        new ContentPartBase64("some-audio", MediaType.AUDIO, "audio/mpeg", Base64.getEncoder().encode("some audio data".getBytes()))
                )),
                new ChatMessage("user", List.of(
                        new ContentPartText("Some other query"),
                        new ContentPartBase64("some-file", MediaType.FILE, "application/pdf", Base64.getEncoder().encode("some pdf data".getBytes())),
                        new ContentPartBase64("some-image-base64", MediaType.IMAGE, "image/png", Base64.getEncoder().encode("some image data".getBytes()))
                ))

        ));

        assertEquals(List.of(
                new ChatMessage("system", "Respond to the user's query"),
                new ChatMessage("user", List.of(
                        new OpenAILLMAdapter.ContentPart("Some query"),
                        new OpenAILLMAdapter.ContentPart(new OpenAILLMAdapter.ImageContent("http://localhost/image")),
                        new OpenAILLMAdapter.ContentPart(
                                new OpenAILLMAdapter.AudioContent(Base64.getEncoder().encodeToString("some audio data".getBytes()),
                                        "mp3"
                                )
                        )
                )),
                new ChatMessage("user", List.of(
                        new OpenAILLMAdapter.ContentPart("Some other query"),
                        new OpenAILLMAdapter.ContentPart(new OpenAILLMAdapter.FileContent(
                                "some-file.pdf",
                                "data:application/pdf;base64,c29tZSBwZGYgZGF0YQ=="
                        )),
                        new OpenAILLMAdapter.ContentPart(new OpenAILLMAdapter.ImageContent("data:image/png;base64,c29tZSBpbWFnZSBkYXRh"))
                ))
        ), formattedMessages);
    }
}
