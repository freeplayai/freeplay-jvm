package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.model.PromptTemplate;
import ai.freeplay.client.processor.FilesystemTemplateResolver;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class FilesystemTemplateResolverTest extends HttpClientTestBase {

    /*
     * This resolver now just delegates to the one in 'thin', so we don't need to go through
     * all the permutations. Just make sure the happy path and one error path work to validate
     * the wire up.
     */

    @Test
    public void testResolvesPromptWithParams() {
        String projectId = "475516c8-7be4-4d55-9388-535cef042981";
        FilesystemTemplateResolver resolver = new FilesystemTemplateResolver(getTestFilesDirectory());
        Collection<PromptTemplate> templates = resolver.getPrompts(projectId, "prod");

        assertEquals(2, templates.size());

        PromptTemplate template = getTemplate(templates, "test-prompt-with-params");

        PromptTemplate expected = new PromptTemplate(
                "test-prompt-with-params",
                "[{\"role\":\"system\",\"content\":\"You are a support agent\",\"kind\":null,\"media_slots\":[]},{\"role\":\"assistant\",\"content\":\"How can I help you?\",\"kind\":null,\"media_slots\":[]},{\"role\":\"user\",\"content\":\"{{question}}\",\"kind\":null,\"media_slots\":[]}]",
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
            assertTrue(e.getMessage().contains("testfiles/prompts_legacy/does_not_exist"));
        }
    }

    @SuppressWarnings("SameParameterValue")
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
        Path path = FileSystems.getDefault().getPath("src", "test", "testfiles", "prompts_legacy");
        assertTrue(path.toFile().exists());
        return path;
    }
}
