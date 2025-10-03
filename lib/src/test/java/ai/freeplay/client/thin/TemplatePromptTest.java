package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.media.MediaInputUrl;
import ai.freeplay.client.thin.resources.prompts.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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

    @Test
    public void testOutputSchemaWithOpenAI() {
        Map<String, Object> outputSchema = new HashMap<>();
        outputSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProperty = new HashMap<>();
        nameProperty.put("type", "string");
        Map<String, Object> ageProperty = new HashMap<>();
        ageProperty.put("type", "integer");
        properties.put("name", nameProperty);
        properties.put("age", ageProperty);
        outputSchema.put("properties", properties);
        outputSchema.put("required", List.of("name"));

        PromptInfo promptInfo = new PromptInfo(
                "test-id",
                "test-version-id",
                "test-template",
                "production",
                Map.of(),
                "openai",
                "gpt-4",
                "openai_chat"
        );

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", "System message"),
                new ChatMessage("user", "User message {{number}}")
        );

        TemplatePrompt templatePrompt = new TemplatePrompt(promptInfo, messages).outputSchema(outputSchema);

        BoundPrompt boundPrompt = templatePrompt.bind(new TemplatePrompt.BindRequest(Map.of("number", 1)));
        FormattedPrompt<?> formattedPrompt = boundPrompt.format();

        assertEquals(outputSchema, formattedPrompt.getOutputSchema());
    }

    @Test
    public void testOutputSchemaPassedThroughFromTemplateToFormattedPrompt() {
        Map<String, Object> outputSchema = new HashMap<>();
        outputSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> titleProperty = new HashMap<>();
        titleProperty.put("type", "string");
        Map<String, Object> ratingProperty = new HashMap<>();
        ratingProperty.put("type", "number");
        properties.put("title", titleProperty);
        properties.put("rating", ratingProperty);
        outputSchema.put("properties", properties);

        PromptInfo promptInfo = new PromptInfo(
                "test-id",
                "test-version-id",
                "test-template",
                "production",
                Map.of(),
                "openai",
                "gpt-4",
                "openai_chat"
        );

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", "System message"),
                new ChatMessage("user", "User message {{number}}")
        );

        TemplatePrompt templatePrompt = new TemplatePrompt(promptInfo, messages).outputSchema(outputSchema);

        assertEquals(outputSchema, templatePrompt.getOutputSchema());

        BoundPrompt boundPrompt = templatePrompt.bind(new TemplatePrompt.BindRequest(Map.of("number", 1)));
        FormattedPrompt<?> formattedPrompt = boundPrompt.format();

        assertEquals(outputSchema, formattedPrompt.getOutputSchema());
    }

    @Test
    public void testOutputSchemaWithUnsupportedProviderThrowsError() {
        Map<String, Object> outputSchema = new HashMap<>();
        outputSchema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> responseProperty = new HashMap<>();
        responseProperty.put("type", "string");
        properties.put("response", responseProperty);
        outputSchema.put("properties", properties);

        PromptInfo promptInfo = new PromptInfo(
                "test-id",
                "test-version-id",
                "test-template",
                "production",
                Map.of(),
                "anthropic",
                "claude-3-opus",
                "anthropic_chat"
        );

        List<ChatMessage> messages = List.of(
                new ChatMessage("user", "User message")
        );

        BoundPrompt boundPrompt = new BoundPrompt(
                promptInfo,
                List.of(new ChatMessage("user", "User message"))
        ).outputSchema(outputSchema);

        FreeplayConfigurationException exception = assertThrows(FreeplayConfigurationException.class, boundPrompt::format);
        assertEquals("Structured outputs are not supported for this model and provider.", exception.getMessage());
    }
}
