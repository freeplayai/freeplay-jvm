package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.internal.v2dto.TemplatesDTO;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class FilesystemTemplateResolverTest extends HttpClientTestBase {

    private final String projectId = "475516c8-7be4-4d55-9388-535cef042981";

    @Test
    public void testResolvesPromptWithParams() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getLegacyTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(2, templates.getPromptTemplates().size());

        TemplateDTO template = getTemplate(templates, "test-prompt-with-params");

        TemplateDTO expected = new TemplateDTO(
                "a8b91d92-e063-4c3e-bb44-0d570793856b",
                "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5",
                "test-prompt-with-params",
                List.of(
                        new TemplateDTO.Message("system", "You are a support agent", List.of()),
                        new TemplateDTO.Message("assistant", "How can I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "openai",
                        "gpt-3.5-turbo-1106",
                        "openai_chat",
                        Map.of("max_tokens", 56,
                                "temperature", 0.1
                        ),
                        Collections.emptyMap()
                ),
                0,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPrompt() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getLegacyTestFilesDirectory());
        TemplateDTO template = resolver.getPrompt(projectId, "test-prompt-with-params", "prod").get();

        TemplateDTO expected = new TemplateDTO(
                "a8b91d92-e063-4c3e-bb44-0d570793856b",
                "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5",
                "test-prompt-with-params",
                List.of(
                        new TemplateDTO.Message("system", "You are a support agent", List.of()),
                        new TemplateDTO.Message("assistant", "How can I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "openai",
                        "gpt-3.5-turbo-1106",
                        "openai_chat",
                        Map.of("max_tokens", 56,
                                "temperature", 0.1
                        ),
                        Collections.emptyMap()
                ),
                0,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptByVersionId() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getLegacyTestFilesDirectory());
        String templateId = "a8b91d92-e063-4c3e-bb44-0d570793856b";
        String templateVersionId = "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5";
        TemplateDTO template = resolver.getPromptByVersionId(projectId, templateId, templateVersionId).get();

        TemplateDTO expected = new TemplateDTO(
                templateId,
                templateVersionId,
                "test-prompt-with-params",
                List.of(
                        new TemplateDTO.Message("system", "You are a support agent", List.of()),
                        new TemplateDTO.Message("assistant", "How can I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "openai",
                        "gpt-3.5-turbo-1106",
                        "openai_chat",
                        Map.of("max_tokens", 56,
                                "temperature", 0.1
                        ),
                        Collections.emptyMap()
                ),
                0,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptWithoutParams() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getLegacyTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(2, templates.getPromptTemplates().size());

        TemplateDTO template = getTemplate(templates, "test-prompt-no-params");

        TemplateDTO expected = new TemplateDTO(
                "5985c6bb-115c-4ca2-99bd-0ffeb917fca4",
                "11e12956-d8d4-448a-af92-66b1dc2155e0",
                "test-prompt-no-params",
                List.of(
                        new TemplateDTO.Message("user", "You are a support agent.", List.of()),
                        new TemplateDTO.Message("assistant", "How may I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "anthropic",
                        "claude-2.1",
                        "anthropic_chat",
                        Collections.emptyMap(),
                        Collections.emptyMap()
                ),
                0,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptInOtherEnvironment() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getLegacyTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "qa").get();

        assertEquals(1, templates.getPromptTemplates().size());

        TemplateDTO template = getTemplate(templates, "test-prompt-with-params");

        TemplateDTO expected = new TemplateDTO(
                "a8b91d92-e063-4c3e-bb44-0d570793856b",
                "5a0fa56d-add1-41e2-b376-8c820bf95903",
                "test-prompt-with-params",
                List.of(
                        new TemplateDTO.Message("system", "You are a support agent", List.of()),
                        new TemplateDTO.Message("assistant", "How can I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "openai",
                        "gpt-3.5-turbo-1106",
                        "openai_chat",
                        Map.of("max_tokens", 56,
                                "temperature", 0.1
                        ),
                        Collections.emptyMap()
                ),
                0,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesV2Prompt() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(5, templates.getPromptTemplates().size());

        TemplateDTO template = getTemplate(templates, "test-prompt");

        TemplateDTO expected = new TemplateDTO(
                "f4758834-9e93-448f-97a4-1cb126f7e328",
                "f4811249-4384-4d71-a1e9-1e8390d5501d",
                "test-prompt",
                List.of(
                        new TemplateDTO.Message("user", "Answer the question to the best of your ability with truthful information, while being entertaining.", List.of()),
                        new TemplateDTO.Message("assistant", "How may I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "anthropic",
                        "claude-2.1",
                        "anthropic_chat",
                        Map.of(
                                "max_tokens", 12,
                                "temperature", 0.15
                        ),
                        Collections.emptyMap()
                ),
                2,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesV2PromptWithHistory() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(5, templates.getPromptTemplates().size());

        TemplateDTO template = getTemplate(templates, "test-prompt-with-history");

        TemplateDTO expected = new TemplateDTO(
                "e8c69d70-1166-4546-8a60-cf4684a3a66b",
                "41ee9a25-0e7a-4c0b-a42b-4b5239721d7b",
                "test-prompt-with-history",
                List.of(
                        new TemplateDTO.Message("user", "Answer the question to the best of your ability with truthful information, while being entertaining.", List.of()),
                        new TemplateDTO.Message(null, null, "history", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "anthropic",
                        "claude-2.1",
                        "anthropic_chat",
                        Map.of(
                                "max_tokens", 12,
                                "temperature", 0.15
                        ),
                        Collections.emptyMap()
                ),
                2,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesV3Prompt() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(5, templates.getPromptTemplates().size());

        TemplateDTO template = getTemplate(templates, "test-prompt-v3");

        TemplateDTO expected = new TemplateDTO(
                "f4758834-9e93-448f-97a4-1cb126f7e328",
                "f4811249-4384-4d71-a1e9-1e8390d5501d",
                "test-prompt-v3",
                List.of(
                        new TemplateDTO.Message("user", "Answer the question to the best of your ability with truthful information, while being entertaining.", List.of()),
                        new TemplateDTO.Message("assistant", "How may I help you?", List.of()),
                        new TemplateDTO.Message("user", "{{question}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "anthropic",
                        "claude-2.1",
                        "anthropic_chat",
                        Map.of(
                                "max_tokens", 12,
                                "temperature", 0.15
                        ),
                        Collections.emptyMap()
                ),
                3,
                projectId
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesV3PromptWithMedia() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();
        TemplateDTO template = getTemplate(templates, "test-prompt-v3-media");

        assertEquals(List.of(
                new TemplateDTO.Message("user", "Answer the question to the best of your ability with truthful information, while being entertaining.", List.of()),
                new TemplateDTO.Message("assistant", "How may I help you?", List.of()),
                new TemplateDTO.Message("user", "{{question}}", List.of(new TemplateDTO.MediaSlot(
                        "image", "some-image"
                )))
        ), template.getContent());
    }

    @Test
    public void testResolvesPromptWithToolSchema() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        TemplatesDTO templates = resolver.getPrompts(projectId, "prod").get();

        TemplateDTO template = getTemplate(templates, "my-anthropic-prompt");

        TemplateDTO expected = new TemplateDTO(
                "6252eca9-edb1-403c-a059-744cfc6e254e",
                "ed3b96f5-0783-4d40-9ef3-35297c29529c",
                "my-anthropic-prompt",
                List.of(
                        new TemplateDTO.Message(null, null, "history", List.of()),
                        new TemplateDTO.Message("user", "What is the weather in {{location}}", List.of())
                ),
                new TemplateDTO.Metadata(
                        "anthropic",
                        "claude-3-sonnet-20240229",
                        "anthropic_chat",
                        Map.of(
                                "max_tokens", 256,
                                "temperature", 0.0
                        ),
                        Collections.emptyMap()
                ),
                3,
                projectId,
                List.of(
                        new TemplateDTO.ToolSchema(
                                "weather_of_location",
                                "Get weather of a location",
                                Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "location", Map.of(
                                                        "type", "string",
                                                        "description", "Location to get the weather for"
                                                )
                                        ),
                                        "required", List.of("location"),
                                        "additionalProperties", false
                                )
                        )
                )
        );

        assertEquals(expected, template);
    }

    @Test
    public void testHandlesDirectoryDoesNotExist() {
        try {
            new FilesystemTemplateResolver(getLegacyTestFilesDirectory().resolve("does_not_exists"));
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains("Path for templates is not a directory"));
            assertTrue(e.getMessage().contains("testfiles/prompts_legacy/does_not_exist"));
        }
    }

    @Test
    public void testHandlesDirectoryIsFile() {
        try {
            new FilesystemTemplateResolver(
                    getLegacyTestFilesDirectory().resolve(
                            "freeplay/prompts_legacy/475516c8-7be4-4d55-9388-535cef042981/prod/test-prompt-with-params.json"));
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains("Path for templates is not a directory"));
            assertTrue(e.getMessage().contains("test-prompt-with-params.json"));
        }
    }

    @Test
    public void testHandlesInvalidDirectory() {
        try {
            // Note /prompts is a valid template directory -- we are one level up to test the failure state.
            new FilesystemTemplateResolver(FileSystems.getDefault().getPath("src", "test", "testfiles"));
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains("Path for templates does not appear to be a Freeplay prompts directory"));
            assertTrue(e.getMessage().contains("src/test/testfiles"));
        }
    }

    @Test
    public void testHandlesEnvironmentNotFound() {
        try {
            FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getLegacyTestFilesDirectory());
            resolver.getPrompts(projectId, "not_an_environment");
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains(
                    "Could not find directory for project 475516c8-7be4-4d55-9388-535cef042981 and environment not_an_environment"));
        }
    }

    private TemplateDTO getTemplate(TemplatesDTO templates, String name) {
        Optional<TemplateDTO> maybeTemplate = getTemplateWithName(templates, name);
        assertTrue(String.format("Unable to find template %s", name), maybeTemplate.isPresent());
        return maybeTemplate.get();
    }

    private Optional<TemplateDTO> getTemplateWithName(
            TemplatesDTO templates,
            String name
    ) {
        return templates.getPromptTemplates().stream()
                .filter((template -> name.equals(template.getPromptTemplateName())))
                .findFirst();
    }

    private Path getLegacyTestFilesDirectory() {
        Path path = FileSystems.getDefault().getPath("src", "test", "testfiles", "prompts_legacy");
        assertTrue(path.toFile().exists());
        return path;
    }

    private Path getTestFilesDirectory() {
        Path path = FileSystems.getDefault().getPath("src", "test", "testfiles", "prompts");
        assertTrue(path.toFile().exists());
        return path;
    }
}
