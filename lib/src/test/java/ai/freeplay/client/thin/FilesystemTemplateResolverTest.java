package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.thin.internal.model.Template;
import ai.freeplay.client.thin.internal.model.Templates;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class FilesystemTemplateResolverTest extends HttpClientTestBase {

    private final String projectId = "475516c8-7be4-4d55-9388-535cef042981";

    @Test
    public void testResolvesPromptWithParams() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Templates templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(3, templates.getTemplates().size());

        Template template = getTemplate(templates, "test-prompt-with-params");

        Template expected = new Template(
                "test-prompt-with-params",
                "[{\"role\":\"system\",\"content\":\"You are a support agent\"},{\"role\":\"assistant\",\"content\":\"How can I help you?\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]",
                "openai_chat",
                "a8b91d92-e063-4c3e-bb44-0d570793856b",
                "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5",
                Map.of("max_tokens", 56,
                        "model", "gpt-3.5-turbo-1106",
                        "temperature", 0.1
                )
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptWithoutParams() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Templates templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(3, templates.getTemplates().size());

        Template template = getTemplate(templates, "test-prompt-no-params");

        Template expected = new Template(
                "test-prompt-no-params",
                "[{\"role\":\"Human\",\"content\":\"You are a support agent.\"},{\"role\":\"Assistant\",\"content\":\"How may I help you?\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]",
                null,
                "5985c6bb-115c-4ca2-99bd-0ffeb917fca4",
                "11e12956-d8d4-448a-af92-66b1dc2155e0",
                Collections.emptyMap()
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptNotChat() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Templates templates = resolver.getPrompts(projectId, "prod").get();

        assertEquals(3, templates.getTemplates().size());

        Template template = getTemplate(templates, "test-prompt-not-chat");

        Template expected = new Template(
                "test-prompt-not-chat",
                "Answer this question: {{question}}",
                null,
                "7f6507a8-fd6a-4925-a985-c77d37dcef96",
                "786293b6-4209-4d2c-9a50-4aadb932be22",
                Collections.emptyMap()
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptInOtherEnvironment() throws ExecutionException, InterruptedException {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Templates templates = resolver.getPrompts(projectId, "qa").get();

        assertEquals(1, templates.getTemplates().size());

        Template template = getTemplate(templates, "test-prompt-with-params");

        Template expected = new Template(
                "test-prompt-with-params",
                "[{\"role\":\"system\",\"content\":\"You are a support agent\"},{\"role\":\"assistant\",\"content\":\"How can I help you?\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]",
                "openai_chat",
                "a8b91d92-e063-4c3e-bb44-0d570793856b",
                "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5",
                Map.of("max_tokens", 56,
                        "model", "gpt-3.5-turbo-1106",
                        "temperature", 0.1
                )
        );

        assertEquals(expected, template);
    }

    @Test
    public void testHandlesDirectoryDoesNotExist() {
        try {
            new FilesystemTemplateResolver(getTestFilesDirectory().resolve("does_not_exists"));
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains("Path for templates is not a directory"));
            assertTrue(e.getMessage().contains("testfiles/prompts/does_not_exist"));
        }
    }

    @Test
    public void testHandlesDirectoryIsFile() {
        try {
            new FilesystemTemplateResolver(
                    getTestFilesDirectory().resolve(
                            "freeplay/prompts/475516c8-7be4-4d55-9388-535cef042981/prod/test-prompt-not-chat.json"));
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains("Path for templates is not a directory"));
            assertTrue(e.getMessage().contains("test-prompt-not-chat.json"));
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
            FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
            resolver.getPrompts(projectId, "notanenvironment");
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains(
                    "Could not find directory for project 475516c8-7be4-4d55-9388-535cef042981 and environment notanenvironment"));
        }
    }

    private Template getTemplate(Templates templates, String name) {
        Optional<Template> maybeTemplate = getTemplateWithName(templates, name);
        assertTrue(maybeTemplate.isPresent());
        return maybeTemplate.get();
    }

    private Optional<Template> getTemplateWithName(
            Templates templates,
            String name
    ) {
        return templates.getTemplates().stream()
                .filter((template -> name.equals(template.getName())))
                .findFirst();
    }

    private Path getTestFilesDirectory() {
        Path path = FileSystems.getDefault().getPath("src", "test", "testfiles", "prompts");
        assertTrue(path.toFile().exists());
        return path;
    }
}
