package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.model.PromptTemplate;
import ai.freeplay.client.processor.FilesystemTemplateResolver;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class FilesystemTemplateResolverTest extends HttpClientTestBase {

    private final String projectId = "475516c8-7be4-4d55-9388-535cef042981";

    @Test
    public void testResolvesPromptWithParams() {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Collection<PromptTemplate> templates = resolver.getPrompts(projectId, "prod");

        assertEquals(3, templates.size());

        PromptTemplate template = getTemplate(templates, "test-prompt-with-params");

        PromptTemplate expected = new PromptTemplate(
                "test-prompt-with-params",
                "[{\"role\":\"system\",\"content\":\"You are a support agent\"},{\"role\":\"assistant\",\"content\":\"How can I help you?\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]",
                "openai_chat",
                "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5",
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
    public void testResolvesPromptWithoutParams() {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Collection<PromptTemplate> templates = resolver.getPrompts(projectId, "prod");

        assertEquals(3, templates.size());

        PromptTemplate template = getTemplate(templates, "test-prompt-no-params");

        PromptTemplate expected = new PromptTemplate(
                "test-prompt-no-params",
                "[{\"role\":\"Human\",\"content\":\"You are a support agent.\"},{\"role\":\"Assistant\",\"content\":\"How may I help you?\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]",
                null,
                "11e12956-d8d4-448a-af92-66b1dc2155e0",
                "5985c6bb-115c-4ca2-99bd-0ffeb917fca4",
                "11e12956-d8d4-448a-af92-66b1dc2155e0",
                Collections.emptyMap()
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptNotChat() {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Collection<PromptTemplate> templates = resolver.getPrompts(projectId, "prod");

        assertEquals(3, templates.size());

        PromptTemplate template = getTemplate(templates, "test-prompt-not-chat");

        PromptTemplate expected = new PromptTemplate(
                "test-prompt-not-chat",
                "Answer this question: {{question}}",
                null,
                "786293b6-4209-4d2c-9a50-4aadb932be22",
                "7f6507a8-fd6a-4925-a985-c77d37dcef96",
                "786293b6-4209-4d2c-9a50-4aadb932be22",
                Collections.emptyMap()
        );

        assertEquals(expected, template);
    }

    @Test
    public void testResolvesPromptInOtherEnvironment() {
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Collection<PromptTemplate> templates = resolver.getPrompts(projectId, "qa");

        assertEquals(1, templates.size());

        PromptTemplate template = getTemplate(templates, "test-prompt-with-params");

        PromptTemplate expected = new PromptTemplate(
                "test-prompt-with-params",
                "[{\"role\":\"system\",\"content\":\"You are a support agent\"},{\"role\":\"assistant\",\"content\":\"How can I help you?\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]",
                "openai_chat",
                "6fe8af2e-defe-41b8-bdf2-7b2ec23592f5",
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
            new FilesystemTemplateResolver(FileSystems.getDefault().getPath("src", "test", "resources"));
            fail("Should have gotten an exception");
        } catch (Exception e) {
            assertEquals(e.getClass(), FreeplayConfigurationException.class);
            assertTrue(e.getMessage().contains("Path for templates does not appear to be a Freeplay prompts directory"));
            assertTrue(e.getMessage().contains("src/test/resources"));
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

    private PromptTemplate getTemplate(Collection<PromptTemplate> templates, String name) {
        Optional<PromptTemplate> maybeTemplate = getTemplateWithName(templates, name);
        assertTrue(maybeTemplate.isPresent());
        return maybeTemplate.get();
    }

    private Optional<PromptTemplate> getTemplateWithName(
            Collection<PromptTemplate> templates,
            String name
    ) {
        return templates.stream()
                .filter((template -> name.equals(template.getName())))
                .findFirst();
    }

    private Path getTestFilesDirectory() {
        Path path = FileSystems.getDefault().getPath("src", "test", "testfiles", "prompts");
        assertTrue(path.toFile().exists());
        return path;
    }
}
