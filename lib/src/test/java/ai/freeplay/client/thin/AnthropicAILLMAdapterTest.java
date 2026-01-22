package ai.freeplay.client.thin;

import ai.freeplay.client.thin.resources.prompts.*;
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
                        new ContentPartText("Some query"),
                        new ContentPartUrl("some-image", MediaType.IMAGE, "http://localhost/image")
                )),
                new ChatMessage("user", List.of(
                        new ContentPartText("Some other query"),
                        new ContentPartBase64("some-file", MediaType.FILE, "application/pdf", Base64.getEncoder().encode("some pdf data".getBytes()))
                ))

        ));

        assertEquals(List.of(
                new ChatMessage("user", List.of(
                        new AnthropicLLMAdapter.ContentPart<Void>("Some query"),
                        new AnthropicLLMAdapter.ContentPart<>("image", new AnthropicLLMAdapter.UrlContent("http://localhost/image"))
                )),
                new ChatMessage("user", List.of(
                        new AnthropicLLMAdapter.ContentPart<Void>("Some other query"),
                        new AnthropicLLMAdapter.ContentPart<>("document", new AnthropicLLMAdapter.Base64Content(
                                "application/pdf",
                                "c29tZSBwZGYgZGF0YQ=="
                        ))
                ))
        ), formattedMessages);
    }
}
