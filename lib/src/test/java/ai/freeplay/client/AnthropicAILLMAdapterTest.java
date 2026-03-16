package ai.freeplay.client;

import ai.freeplay.client.adapters.*;

import ai.freeplay.client.resources.prompts.*;
import org.junit.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AnthropicAILLMAdapterTest {
    @Test
    public void testToLLMSyntax() {
        LLMAdapters.LLMAdapter<?> adapter = LLMAdapters.adapterForFlavor("anthropic_chat");

        Object formattedMessages = adapter.toLLMSyntax(List.of(
                new ChatMessage("system", "Respond to the user's query"),
                new ChatMessage("user", List.of(
                        new TextContent("Some query"),
                        new ImageUrlContent("http://localhost/image", "image")
                )),
                new ChatMessage("user", List.of(
                        new TextContent("Some other query"),
                        new FileContent("application/pdf", Base64.getEncoder().encodeToString("some pdf data".getBytes()), "some-file")
                ))

        ));

        assertEquals(List.of(
                new ChatMessage("user", List.of(
                        new AnthropicLLMAdapter.AnthropicContentPart<Void>("Some query"),
                        new AnthropicLLMAdapter.AnthropicContentPart<>("image", new AnthropicLLMAdapter.UrlContent("http://localhost/image"))
                )),
                new ChatMessage("user", List.of(
                        new AnthropicLLMAdapter.AnthropicContentPart<Void>("Some other query"),
                        new AnthropicLLMAdapter.AnthropicContentPart<>("document", new AnthropicLLMAdapter.Base64Content(
                                "application/pdf",
                                "c29tZSBwZGYgZGF0YQ=="
                        ))
                ))
        ), formattedMessages);
    }
}
