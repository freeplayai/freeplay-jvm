package ai.freeplay.client.processor;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.PromptTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class FilesystemTemplateResolver implements TemplateResolver {
    private final Path promptsDirectory;

    public FilesystemTemplateResolver(Path rootDirectory) {
        if (!Files.isDirectory(rootDirectory)) {
            throw new FreeplayConfigurationException(format(
                    "Path for templates is not a directory. [%s]%n",
                    rootDirectory.toAbsolutePath()));
        }
        promptsDirectory = rootDirectory.resolve("freeplay/prompts");
        if (!Files.isDirectory(promptsDirectory)) {
            throw new FreeplayConfigurationException(format(
                    "Path for templates does not appear to be a Freeplay prompts directory. [%s]%n",
                    rootDirectory.toAbsolutePath()));
        }
    }

    @Override
    public Collection<PromptTemplate> getPrompts(String projectId, String environment) {
        Path environmentDir = promptsDirectory.resolve(projectId + "/" + environment);
        if (!Files.exists(environmentDir)) {
            throw new FreeplayConfigurationException(format(
                    "Could not find directory for project %s and environment %s.%n",
                    projectId, environment));
        }
        return Arrays.stream(
                        requireNonNull(
                                environmentDir.toFile().listFiles((dir, name) -> name.endsWith(".json"))
                        )).map(this::toPromptTemplate)
                .collect(Collectors.toList());
    }

    private PromptTemplate toPromptTemplate(File templateFile) {
        File promptAbsoluteFile = templateFile.getAbsoluteFile();

        Map<String, Object> docMap;
        try {
            docMap = JSONUtil.parseMap(Files.readString(promptAbsoluteFile.toPath()));
        } catch (IOException | FreeplayException e) {
            throw new FreeplayConfigurationException(
                    format("Unable to read prompt file %s.%n", promptAbsoluteFile.toPath()), e);
        }
        //noinspection unchecked
        Map<String, Object> metdataMap = (Map<String, Object>) docMap.get("metadata");

        //noinspection unchecked
        return new PromptTemplate(
                (String) docMap.get("name"),
                (String) docMap.get("content"),
                (String) metdataMap.get("flavor_name"),
                (String) docMap.get("prompt_template_version_id"),
                (String) docMap.get("prompt_template_id"),
                (String) docMap.get("prompt_template_version_id"),
                (Map<String, Object>) metdataMap.get("params")
        );
    }
}
