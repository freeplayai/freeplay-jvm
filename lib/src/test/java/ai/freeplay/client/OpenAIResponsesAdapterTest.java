package ai.freeplay.client;

import ai.freeplay.client.adapters.*;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.resources.prompts.*;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class OpenAIResponsesAdapterTest {

    // -- Adapter unit tests --

    @Test
    public void testStripsSystemMessagesAndWrapsWithType() {
        OpenAIResponsesAdapter adapter = new OpenAIResponsesAdapter();

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(
                new ChatMessage("system", "You are helpful."),
                new ChatMessage("user", "Hello"),
                new ChatMessage("assistant", "Hi there")
        ));

        assertEquals(2, result.size());
        assertEquals(Map.of("type", "message", "role", "user", "content", "Hello"), result.get(0));
        assertEquals(Map.of("type", "message", "role", "assistant", "content", "Hi there"), result.get(1));
    }

    @Test
    public void testPassesThroughDeveloperRole() {
        OpenAIResponsesAdapter adapter = new OpenAIResponsesAdapter();

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(
                new ChatMessage("developer", "Be concise."),
                new ChatMessage("user", "Hello")
        ));

        assertEquals(2, result.size());
        assertEquals(Map.of("type", "message", "role", "developer", "content", "Be concise."), result.get(0));
        assertEquals(Map.of("type", "message", "role", "user", "content", "Hello"), result.get(1));
    }

    @Test
    public void testProviderReturnsOpenai() {
        assertEquals("openai", new OpenAIResponsesAdapter().getProvider());
    }

    @Test
    public void testRoleSupportIsOpenAIResponses() {
        assertEquals(RoleSupport.OPENAI_RESPONSES, new OpenAIResponsesAdapter().getRoleSupport());
    }

    @Test
    public void testStructuredMediaContentUsesResponsesApiTypes() {
        OpenAIResponsesAdapter adapter = new OpenAIResponsesAdapter();

        List<Object> structuredContent = List.of(
                new TextContent("Describe this image"),
                new ImageUrlContent("https://example.com/photo.jpg", "image"),
                new ImageContent("image/png", "iVBOR"),
                new FileContent("application/pdf", "JVBERi", "doc")
        );

        List<Map<String, Object>> result = adapter.toLLMSyntax(List.of(
                new ChatMessage("user", structuredContent)
        ));

        assertEquals(1, result.size());
        Map<String, Object> msg = result.get(0);
        assertEquals("message", msg.get("type"));
        assertEquals("user", msg.get("role"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) msg.get("content");
        assertEquals(4, content.size());

        // Text part
        assertEquals("input_text", content.get(0).get("type"));
        assertEquals("Describe this image", content.get(0).get("text"));

        // Image URL part
        assertEquals("input_image", content.get(1).get("type"));
        assertEquals("https://example.com/photo.jpg", content.get(1).get("image_url"));

        // Image base64 part
        assertEquals("input_image", content.get(2).get("type"));
        assertEquals("data:image/png;base64,iVBOR", content.get(2).get("image_url"));

        // File base64 part
        assertEquals("input_file", content.get(3).get("type"));
        assertEquals("doc.pdf", content.get(3).get("filename"));
        assertEquals("data:application/pdf;base64,JVBERi", content.get(3).get("file_data"));
    }

    // -- Tool schema (flat format) --

    @Test
    public void testFlatToolSchemaFormat() {
        OpenAIResponsesAdapter adapter = new OpenAIResponsesAdapter();

        List<ToolSchema> toolSchema = List.of(
                new ToolSchema("get_weather", "Get the weather", Map.of("type", "object"))
        );

        List<Map<String, Object>> result = adapter.toToolSchemaFormat(toolSchema);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("type", "function");
        expected.put("name", "get_weather");
        expected.put("description", "Get the weather");
        expected.put("parameters", Map.of("type", "object"));

        assertEquals(List.of(expected), result);
    }

    @Test
    public void testNullToolSchemaReturnsNull() {
        assertNull(new OpenAIResponsesAdapter().toToolSchemaFormat(null));
    }

    // -- Output schema --

    @Test
    public void testOutputSchemaPassthrough() {
        Map<String, Object> schema = Map.of("type", "json_schema", "json_schema", Map.of("name", "Output"));
        assertEquals(schema, new OpenAIResponsesAdapter().toOutputSchemaFormat(schema));
    }

    // -- Role coercion via prepareMessages --

    @Test
    public void testDeveloperPassesThroughForOpenAIResponses() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("developer", "Be concise."),
                new ChatMessage("user", "Hello")
        );

        List<ChatMessage> result = RoleSupport.prepareMessages(
                messages, RoleSupport.OPENAI_RESPONSES, "openai_responses");

        assertEquals(messages, result);
    }

    @Test
    public void testDeveloperCoercedToSystemForOpenAIChat() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("developer", "Be concise."),
                new ChatMessage("user", "Hello")
        );

        List<ChatMessage> result = RoleSupport.prepareMessages(
                messages, RoleSupport.OPENAI, "openai_chat");

        assertEquals("system", result.get(0).getRole());
        assertEquals("Be concise.", result.get(0).getContent());
        assertEquals("user", result.get(1).getRole());
    }

    @Test(expected = FreeplayConfigurationException.class)
    public void testDeveloperThrowsForAnthropic() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("developer", "Be concise.")
        );

        RoleSupport.prepareMessages(messages, RoleSupport.DEFAULT, "anthropic_chat");
    }

    @Test
    public void testToolRolePassesThroughForOpenAIChat() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("user", "Hello"),
                new ChatMessage("tool", "{\"result\": 42}")
        );

        List<ChatMessage> result = RoleSupport.prepareMessages(
                messages, RoleSupport.OPENAI, "openai_chat");

        assertEquals(messages, result);
    }

    @Test
    public void testToolRolePassesThroughForOpenAIResponses() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("user", "Hello"),
                new ChatMessage("tool", "{\"result\": 42}")
        );

        List<ChatMessage> result = RoleSupport.prepareMessages(
                messages, RoleSupport.OPENAI_RESPONSES, "openai_responses");

        assertEquals(messages, result);
    }

    @Test(expected = FreeplayConfigurationException.class)
    public void testToolRoleThrowsForAnthropic() {
        List<ChatMessage> messages = List.of(
                new ChatMessage("tool", "{\"result\": 42}")
        );

        RoleSupport.prepareMessages(messages, RoleSupport.DEFAULT, "anthropic_chat");
    }

    // -- End-to-end formatting via BoundPrompt --

    @Test
    public void testFormatsPromptWithSystemExtracted() {
        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "openai", "gpt-4", "openai_responses"),
                List.of(
                        new ChatMessage("system", "You are helpful."),
                        new ChatMessage("user", "Question: {{question}}")
                )
        );

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "Why?")))
                .format();

        assertEquals("You are helpful.", formatted.getSystemContent().orElse(null));

        List<Map<String, Object>> prompt = formatted.getFormattedPrompt();
        assertEquals(1, prompt.size());
        assertEquals("message", prompt.get(0).get("type"));
        assertEquals("user", prompt.get(0).get("role"));
        assertEquals("Question: Why?", prompt.get(0).get("content"));
    }

    @Test
    public void testFormatsPromptWithDeveloperRole() {
        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "openai", "gpt-4", "openai_responses"),
                List.of(
                        new ChatMessage("developer", "Be concise."),
                        new ChatMessage("user", "{{question}}")
                )
        );

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "Why?")))
                .format();

        List<Map<String, Object>> prompt = formatted.getFormattedPrompt();
        assertEquals(2, prompt.size());
        assertEquals("developer", prompt.get(0).get("role"));
        assertEquals("Be concise.", prompt.get(0).get("content"));
    }

    @Test
    public void testAllMessagesAppendsMessage() {
        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "openai", "gpt-4", "openai_responses"),
                List.of(
                        new ChatMessage("system", "You are helpful."),
                        new ChatMessage("user", "{{question}}")
                )
        );

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "Why?")))
                .format();

        List<ChatMessage> all = formatted.allMessages(new ChatMessage("assistant", "Let me help."));

        assertEquals(3, all.size());
        assertEquals("system", all.get(0).getRole());
        assertEquals("user", all.get(1).getRole());
        assertEquals("assistant", all.get(2).getRole());
    }

    @Test
    public void testAllMessagesAcceptsList() {
        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "openai", "gpt-4", "openai_responses"),
                List.of(new ChatMessage("user", "{{question}}"))
        );

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "Why?")))
                .format();

        Map<String, Object> outputMessage = Map.of(
                "type", "message",
                "role", "assistant",
                "content", List.of(Map.of("type", "output_text", "text", "Let me help."))
        );

        List<ChatMessage> all = formatted.allMessages(List.of(outputMessage));

        assertEquals(2, all.size());
        assertTrue(all.get(1).isCompletionMessage());
        assertEquals(outputMessage, all.get(1).getCompletionMessage());
    }

    @Test
    public void testOpenAIResponsesToolSchemaFormatting() {
        List<ToolSchema> toolSchema = List.of(
                new ToolSchema("get_weather", "Get the weather for a location", Map.of(
                        "type", "object",
                        "properties", Map.of("location", Map.of("type", "string")),
                        "required", List.of("location")
                ))
        );

        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "openai", "gpt-4", "openai_responses"),
                getChatPromptContentObjects().stream()
                        .map(obj -> new ChatMessage(
                                (String) ((Map<?, ?>) obj).get("role"),
                                (String) ((Map<?, ?>) obj).get("content")
                        ))
                        .collect(toList()),
                toolSchema
        );

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "test")))
                .format("openai_responses");

        List<Map<String, Object>> result = formatted.getToolSchema();
        assertEquals(1, result.size());
        assertEquals("function", result.get(0).get("type"));
        assertEquals("get_weather", result.get(0).get("name"));
        assertEquals("Get the weather for a location", result.get(0).get("description"));
        assertNotNull(result.get(0).get("parameters"));
    }

    @Test
    public void testEffectiveFlavorAndProviderWhenOverridden() {
        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "anthropic", "claude-3", "anthropic_chat"),
                List.of(new ChatMessage("user", "{{question}}"))
        );

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "test")))
                .format("openai_responses");

        assertEquals("openai_responses", formatted.getPromptInfo().getFlavorName());
        assertEquals("openai", formatted.getPromptInfo().getProvider());
    }

    @Test
    public void testOutputSchemaSupportedForOpenAIResponses() {
        TemplatePrompt templatePrompt = new TemplatePrompt(
                new PromptInfo(promptTemplateId, promptTemplateVersionId, "test", "prod",
                        Map.of(), "openai", "gpt-4", "openai_responses"),
                List.of(new ChatMessage("user", "{{question}}"))
        );
        templatePrompt.outputSchema(Map.of(
                "type", "json_schema",
                "json_schema", Map.of("name", "Output", "strict", true, "schema", Map.of())
        ));

        FormattedPrompt<List<Map<String, Object>>> formatted = templatePrompt
                .bind(new TemplatePrompt.BindRequest(Map.of("question", "test")))
                .format("openai_responses");

        assertNotNull(formatted.getOutputSchema());
        assertEquals("json_schema", formatted.getOutputSchema().get("type"));
    }
}
