package ai.freeplay.client.thin;

import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.media.MediaInputUrl;
import ai.freeplay.client.thin.resources.prompts.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TemplatePromptTest {
    @Test
    public void testBind() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("user", "{{query}}")
        );

        TemplatePrompt prompt = new TemplatePrompt(null, messages);

        BoundPrompt boundPrompt = prompt.bind(new TemplatePrompt.BindRequest(Map.of("query", "Some query")));
        assertEquals(List.of(new ChatMessage("user", "Some query")), boundPrompt.getMessages());
    }

    @Test
    public void testBindWithMedia() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", "Respond to the user's query"),
                new ChatMessage("user", "{{query}}", List.of(
                        new MediaSlot(MediaType.IMAGE, "some-image"),
                        new MediaSlot(MediaType.AUDIO, "some-audio")
                ))
        );

        TemplatePrompt prompt = new TemplatePrompt(null, messages);
        MediaInputCollection mediaInputs = new MediaInputCollection();
        mediaInputs.put("some-image", new MediaInputUrl("http://localhost/image"));
        mediaInputs.put("some-audio", new MediaInputBase64("some data".getBytes(), "audio/mpeg"));

        BoundPrompt boundPrompt = prompt.bind(new TemplatePrompt.BindRequest(Map.of("query", "Some query")).mediaInputs(mediaInputs));

        assertEquals(List.of(
                new ChatMessage("system", "Respond to the user's query"),
                new ChatMessage("user", List.of(
                        new ContentPartText("Some query"),
                        new ContentPartUrl("some-image", MediaType.IMAGE, "http://localhost/image"),
                        new ContentPartBase64("some-audio", MediaType.AUDIO, "audio/mpeg", "some data".getBytes())
                ))
        ), boundPrompt.getMessages());
    }
}
