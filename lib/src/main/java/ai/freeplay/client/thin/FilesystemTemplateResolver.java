package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.internal.model.LocalTemplate;
import ai.freeplay.client.thin.internal.model.Template;
import ai.freeplay.client.thin.internal.model.Templates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<Templates> getPrompts(String projectId, String environment) {
        Path environmentDir = promptsDirectory.resolve(projectId + "/" + environment);
        if (!Files.exists(environmentDir)) {
            throw new FreeplayConfigurationException(format(
                    "Could not find directory for project %s and environment %s.%n",
                    projectId, environment));
        }
        List<Template> templateList = Arrays.stream(
                        requireNonNull(
                                environmentDir.toFile().listFiles((dir, name) -> name.endsWith(".json"))
                        )).map(this::toTemplate)
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(new Templates(templateList));
    }

    private Template toTemplate(File templateFile) {
        File promptAbsoluteFile = templateFile.getAbsoluteFile();

        try {
            LocalTemplate localTemplate = JSONUtil.parse(Files.readString(promptAbsoluteFile.toPath()), LocalTemplate.class);
            return new Template(
                    localTemplate.getName(),
                    localTemplate.getContent(),
                    localTemplate.getMetadata().getFlavorName(),
                    localTemplate.getPromptTemplateId(),
                    localTemplate.getPromptTemplateVersionId(),
                    localTemplate.getMetadata().getParams()
            );
        } catch (IOException e) {
            throw new FreeplayConfigurationException("Unable to read prompt template. ", e);
        }
    }
}
