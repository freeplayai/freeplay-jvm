package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.media.MediaInputBase64;
import ai.freeplay.client.media.MediaInputCollection;
import ai.freeplay.client.media.MediaInputUrl;
import ai.freeplay.client.model.Provider;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.resources.prompts.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class TemplatePromptTest extends HttpClientTestBase {
    private final String templateName = "my-prompt-template";

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

        BoundPrompt boundPrompt = new BoundPrompt(
                promptInfo,
                List.of(new ChatMessage("user", "User message"))
        ).outputSchema(outputSchema);

        FreeplayConfigurationException exception = assertThrows(FreeplayConfigurationException.class, boundPrompt::format);
        assertEquals("Structured outputs are not supported for this model and provider.", exception.getMessage());
    }

    @Test
    public void testCreatePromptVersionMinimal() {
        withMockedClient((HttpClient mockedClient) -> {
            String modelName = "claude-4-sonnet-20250514";
            String newPromptTemplateId = UUID.randomUUID().toString();
            String newPromptTemplateVersionId = UUID.randomUUID().toString();

            mockCreatePromptVersionAsync(mockedClient, templateName, newPromptTemplateId, newPromptTemplateVersionId);

            Freeplay fpClient = new Freeplay(Freeplay.Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            List<TemplateDTO.Message> templateMessages = List.of(
                    new TemplateDTO.Message("user", "Answer this question as concisely as you can: {{question}}")
            );

            CreateVersionRequest request = new CreateVersionRequest.Builder(
                    projectId,
                    templateName,
                    templateMessages,
                    modelName,
                    Provider.Anthropic
            ).build();

            TemplateVersionResponse result = fpClient.prompts().createVersion(request).get();

            // Verify the HTTP request was made
            ArgumentCaptor<HttpRequest> requestArg = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mockedClient).sendAsync(requestArg.capture(), any());
            assertTrue(requestArg.getValue().uri().toString().contains(
                    String.format("/v2/projects/%s/prompt-templates/name/%s/versions", projectId, templateName)
            ));
            assertEquals("POST", requestArg.getValue().method());

            // Verify response fields
            assertEquals(newPromptTemplateId, result.getPromptTemplateId());
            assertEquals(newPromptTemplateVersionId, result.getPromptTemplateVersionId());
            assertEquals(templateName, result.getPromptTemplateName());
            assertEquals("v1.0", result.getVersionName());
            assertEquals("Test version", result.getVersionDescription());
            assertEquals(2, result.getFormatVersion());
            assertEquals(projectId, result.getProjectId());

            // Verify metadata
            assertNotNull(result.getMetadata());
            assertEquals("anthropic", result.getMetadata().getProvider());
            assertEquals("claude-3-5-sonnet-20241022", result.getMetadata().getModel());
            assertEquals("anthropic_chat", result.getMetadata().getFlavor());
            assertNotNull(result.getMetadata().getParams());
            assertEquals(0.7, ((Number) result.getMetadata().getParams().get("temperature")).doubleValue(), 0.01);
            assertNotNull(result.getMetadata().getProviderInfo());

            // Verify content
            assertNotNull(result.getContent());
            assertEquals(1, result.getContent().size());
            assertEquals("user", result.getContent().get(0).getRole());
            assertEquals("Answer this question as concisely as you can: {{question}}", result.getContent().get(0).getContent());

            // Verify tool schema
            assertNotNull(result.getToolSchema());
            assertEquals(0, result.getToolSchema().size());
        });
    }

    @Test
    public void testCreatePromptVersionAllFields() {
        withMockedClient((HttpClient mockedClient) -> {
            String modelName = "claude-4-sonnet-20250514";
            String versionName = "v2.1";
            String versionDescription = "Updated version with tool schema";
            String newPromptTemplateId = UUID.randomUUID().toString();
            String newPromptTemplateVersionId = UUID.randomUUID().toString();

            mockCreatePromptVersionAsync(mockedClient, templateName, newPromptTemplateId, newPromptTemplateVersionId);

            Freeplay fpClient = new Freeplay(Freeplay.Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            List<TemplateDTO.Message> templateMessages = List.of(
                    new TemplateDTO.Message("user", "Answer this question as concisely as you can: {{question}}")
            );

            Map<String, Object> llmParameters = Map.of(
                    "temperature", 0.7,
                    "max_tokens", 1000
            );

            List<TemplateDTO.ToolSchema> toolSchema = List.of(
                    new TemplateDTO.ToolSchema("get_weather", "Get the weather for a location", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "location", Map.of("type", "string"),
                                    "unit", Map.of("type", "string", "enum", List.of("celsius", "fahrenheit"))
                            ),
                            "required", List.of("location")
                    ))
            );

            List<String> environments = List.of("development", "staging");

            CreateVersionRequest request = new CreateVersionRequest.Builder(
                    projectId,
                    templateName,
                    templateMessages,
                    modelName,
                    Provider.Anthropic
            )
                    .versionName(versionName)
                    .versionDescription(versionDescription)
                    .llmParameters(llmParameters)
                    .toolSchema(toolSchema)
                    .environments(environments)
                    .build();

            TemplateVersionResponse result = fpClient.prompts().createVersion(request).get();

            // Verify the HTTP request was made
            ArgumentCaptor<HttpRequest> requestArg = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mockedClient).sendAsync(requestArg.capture(), any());
            assertTrue(requestArg.getValue().uri().toString().contains(
                    String.format("/v2/projects/%s/prompt-templates/name/%s/versions", projectId, templateName)
            ));
            assertEquals("POST", requestArg.getValue().method());

            // Verify response fields
            assertEquals(newPromptTemplateId, result.getPromptTemplateId());
            assertEquals(newPromptTemplateVersionId, result.getPromptTemplateVersionId());
            assertEquals(templateName, result.getPromptTemplateName());
            assertEquals("v1.0", result.getVersionName());
            assertEquals("Test version", result.getVersionDescription());
            assertEquals(2, result.getFormatVersion());
            assertEquals(projectId, result.getProjectId());

            // Verify metadata
            assertNotNull(result.getMetadata());
            assertEquals("anthropic", result.getMetadata().getProvider());
            assertEquals("claude-3-5-sonnet-20241022", result.getMetadata().getModel());
            assertEquals("anthropic_chat", result.getMetadata().getFlavor());
            assertNotNull(result.getMetadata().getParams());
            assertEquals(0.7, ((Number) result.getMetadata().getParams().get("temperature")).doubleValue(), 0.01);
            assertNotNull(result.getMetadata().getProviderInfo());

            // Verify content
            assertNotNull(result.getContent());
            assertEquals(1, result.getContent().size());
            assertEquals("user", result.getContent().get(0).getRole());
            assertEquals("Answer this question as concisely as you can: {{question}}", result.getContent().get(0).getContent());

            // Verify tool schema (empty array in mock)
            assertNotNull(result.getToolSchema());
            assertEquals(0, result.getToolSchema().size());
        });
    }

    @Test
    public void testUpdateVersionEnvironments() {
        withMockedClient((HttpClient mockedClient) -> {
            List<String> environments = List.of("dev", "prod");
            String testPromptTemplateId = UUID.randomUUID().toString();
            String testPromptTemplateVersionId = UUID.randomUUID().toString();

            mockUpdateVersionEnvironmentsAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Freeplay.Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            fpClient.prompts().updateVersionEnvironments(
                    projectId,
                    testPromptTemplateId,
                    testPromptTemplateVersionId,
                    environments
            ).get();

            ArgumentCaptor<HttpRequest> requestArg = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mockedClient).sendAsync(requestArg.capture(), any());
            assertTrue(requestArg.getValue().uri().toString().contains(
                    String.format("/v2/projects/%s/prompt-templates/id/%s/versions/%s/environments",
                            projectId, testPromptTemplateId, testPromptTemplateVersionId)
            ));
            assertEquals("POST", requestArg.getValue().method());
        });
    }

    @Test
    public void testUpdateVersionEnvironmentsErrorsOnInvalidProjectId() {
        withMockedClient((HttpClient mockedClient) -> {
            List<String> environments = List.of("dev", "prod");
            String testPromptTemplateId = UUID.randomUUID().toString();
            String testPromptTemplateVersionId = UUID.randomUUID().toString();
            String invalidProjectId = UUID.randomUUID().toString();

            mockUpdateVersionEnvironmentsAsyncUnauthorized(mockedClient);

            Freeplay fpClient = new Freeplay(Freeplay.Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.prompts().updateVersionEnvironments(
                            invalidProjectId,
                            testPromptTemplateId,
                            testPromptTemplateVersionId,
                            environments
                    ).get()
            );

            assertTrue(exception.getCause().getMessage().contains("Error making call [400]"));
        });
    }
}
