package ai.freeplay.client;

import ai.freeplay.client.adapters.*;

import ai.freeplay.client.resources.prompts.*;
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
                        new TextContent("Some query"),
                        new ImageUrlContent("http://localhost/image", "image"),
                        new AudioContent("audio/mpeg", Base64.getEncoder().encodeToString("some audio data".getBytes()))
                )),
                new ChatMessage("user", List.of(
                        new TextContent("Some other query"),
                        new FileContent("application/pdf", Base64.getEncoder().encodeToString("some pdf data".getBytes()), "some-file"),
                        new ImageContent("image/png", Base64.getEncoder().encodeToString("some image data".getBytes()))
                ))

        ));

        assertEquals(List.of(
                new ChatMessage("system", "Respond to the user's query"),
                new ChatMessage("user", List.of(
                        new OpenAILLMAdapter.OpenAIContentPart("Some query"),
                        new OpenAILLMAdapter.OpenAIContentPart(new OpenAILLMAdapter.OpenAIImageContent("http://localhost/image")),
                        new OpenAILLMAdapter.OpenAIContentPart(
                                new OpenAILLMAdapter.OpenAIAudioContent(Base64.getEncoder().encodeToString("some audio data".getBytes()),
                                        "mp3"
                                )
                        )
                )),
                new ChatMessage("user", List.of(
                        new OpenAILLMAdapter.OpenAIContentPart("Some other query"),
                        new OpenAILLMAdapter.OpenAIContentPart(new OpenAILLMAdapter.OpenAIFileContent(
                                "some-file.pdf",
                                "data:application/pdf;base64,c29tZSBwZGYgZGF0YQ=="
                        )),
                        new OpenAILLMAdapter.OpenAIContentPart(new OpenAILLMAdapter.OpenAIImageContent("data:image/png;base64,c29tZSBpbWFnZSBkYXRh"))
                ))
        ), formattedMessages);
    }
}
