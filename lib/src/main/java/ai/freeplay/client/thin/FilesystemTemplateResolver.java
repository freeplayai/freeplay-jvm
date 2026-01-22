package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO.ToolSchema;
import ai.freeplay.client.thin.internal.v2dto.TemplatesDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.JSONUtil.nodeToMap;
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
    public CompletableFuture<TemplatesDTO> getPrompts(String projectId, String environment) {
        Path environmentDir = getEnvironmentDir(projectId, environment);
        List<TemplateDTO> templateList = Arrays.stream(
                        requireNonNull(
                                environmentDir.toFile().listFiles((dir, name) -> name.endsWith(".json"))
                        )).map(file -> toTemplate(file, projectId))
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(new TemplatesDTO(templateList));
    }

    @Override
    public CompletableFuture<TemplateDTO> getPrompt(String projectId, String templateName, String environment) {
        Path environmentDir = getEnvironmentDir(projectId, environment);
        Path templateFile = environmentDir.resolve(templateName + ".json");
        if (!Files.exists(templateFile)) {
            throw new FreeplayConfigurationException(format(
                    "Could not find template %s for project %s and environment %s.%n",
                    templateName,
                    projectId,
                    environment
            ));
        }

        return CompletableFuture.completedFuture(toTemplate(templateFile.toFile(), projectId));
    }

    public CompletableFuture<TemplateDTO> getPromptByVersionId(String projectId, String templateId, String templateVersionId) {
        try {
            List<Path> promptFilePaths = getAllPromptFilePaths(projectId);
            for (Path templateFile : promptFilePaths) {
                TemplateDTO templatePrompt = toTemplate(templateFile.toFile(), projectId);
                if (templatePrompt.getPromptTemplateVersionId().equals(templateVersionId)) {
                    return CompletableFuture.completedFuture(templatePrompt);
                }
            }
            throw new FreeplayConfigurationException(format(
                    "Could not find template version id %s for project %s in local filesystem",
                    templateVersionId,
                    projectId
            ));
        } catch (IOException e) {
            System.err.println("Failed to retrieve prompt file paths: " + e.getMessage());
            throw new RuntimeException("Error accessing prompt files", e);
        }


    }

    private Path getEnvironmentDir(String projectId, String environment) {
        Path environmentDir = promptsDirectory.resolve(projectId + "/" + environment);
        if (!Files.exists(environmentDir)) {
            throw new FreeplayConfigurationException(format(
                    "Could not find directory for project %s and environment %s.%n",
                    projectId, environment));
        }
        return environmentDir;
    }

    private List<Path> getAllPromptFilePaths(String projectId) throws IOException {
        Path projectDir = promptsDirectory.resolve(projectId);

        if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
            throw new FreeplayConfigurationException(String.format(
                    "Could not find directory for project %s.%n", projectId));
        }

        try (Stream<Path> paths = Files.walk(projectDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());
        }
    }

    private TemplateDTO toTemplate(File templateFile, String projectId) {
        File promptAbsoluteFile = templateFile.getAbsoluteFile();

        try {
            JsonNode templateNode = JSONUtil.parseDOM(Files.readString(promptAbsoluteFile.toPath()));
            if (isFormat(templateNode, 2) | isFormat(templateNode, 3)) {
                return new TemplateDTO(
                        templateNode.get("prompt_template_id").textValue(),
                        templateNode.get("prompt_template_version_id").textValue(),
                        templateNode.get("prompt_template_name").textValue(),
                        getV2Content(templateNode.get("content")),
                        new TemplateDTO.Metadata(
                                templateNode.path("metadata").path("provider").textValue(),
                                templateNode.path("metadata").path("model").textValue(),
                                templateNode.path("metadata").path("flavor").textValue(),
                                nodeToMap(templateNode.path("metadata").path("params")),
                                nodeToMap(templateNode.path("metadata").path("provider_info"))
                        ),
                        templateNode.get("format_version").asInt(),
                        projectId,
                        extractToolSchemas(templateNode.path("tool_schema"))
                );
            } else {
                String flavorName = templateNode.path("metadata").path("flavor_name").textValue();
                if (flavorName == null) {
                    throw new FreeplayConfigurationException(format(
                            "Flavor for prompt %s must be configured in the Freeplay UI. Unable to fulfill request.",
                            templateNode.get("name").textValue())
                    );
                }
                LLMAdapters.LLMAdapter<?> llmAdapter = LLMAdapters.adapterForFlavor(flavorName);

                String model = templateNode.path("metadata").path("params").path("model").textValue();
                if (model == null) {
                    throw new FreeplayConfigurationException(format(
                            "Model for prompt %s must be configured in the Freeplay UI. Unable to fulfill request.",
                            templateNode.get("name").textValue())
                    );
                }
                Map<String, Object> params = nodeToMap(templateNode.path("metadata").path("params"));
                params.remove("model");

                return new TemplateDTO(
                        templateNode.get("prompt_template_id").textValue(),
                        templateNode.get("prompt_template_version_id").textValue(),
                        templateNode.get("name").textValue(),
                        getV2Content(JSONUtil.parseDOM(templateNode.get("content").asText())),
                        new TemplateDTO.Metadata(
                                llmAdapter.getProvider(),
                                model,
                                flavorName,
                                params,
                                Collections.emptyMap()
                        ),
                        0,
                        projectId,
                        extractToolSchemas(templateNode.path("tool_schema"))
                );
            }
        } catch (IOException e) {
            throw new FreeplayConfigurationException("Unable to read prompt template. ", e);
        }
    }

    private static List<ToolSchema> extractToolSchemas(JsonNode toolSchemaNode) {
        return Optional.ofNullable(toolSchemaNode)
                .filter(JsonNode::isArray)
                .map(schemas -> {
                    List<ToolSchema> toolSchemas = new ArrayList<>();
                    schemas.forEach(schema -> toolSchemas.add(new ToolSchema(
                            schema.path("name").textValue(),
                            schema.path("description").textValue(),
                            nodeToMap(schema.path("parameters"))
                    )));
                    return toolSchemas;
                })
                .orElse(Collections.emptyList());
    }

    private List<TemplateDTO.Message> getV2Content(JsonNode content) {
        List<TemplateDTO.Message> messages = new ArrayList<>(content.size());
        content.forEach(messageNode -> {
            if (messageNode.path("kind").isMissingNode()) {
                JsonNode mediaNode = messageNode.get("media_slots");

                List<TemplateDTO.MediaSlot> mediaSlots = new ArrayList<>();
                if (mediaNode != null && mediaNode.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) mediaNode;
                    for (JsonNode item : arrayNode) {
                        mediaSlots.add(new TemplateDTO.MediaSlot(
                                item.path("type").textValue(),
                                item.get("placeholder_name").textValue()
                        ));
                    }
                }

                messages.add(
                        new TemplateDTO.Message(
                                translateRole(messageNode.get("role").textValue()),
                                messageNode.get("content").textValue(),
                                mediaSlots
                        )
                );
            } else {
                messages.add(
                        new TemplateDTO.Message(
                                null,
                                null,
                                messageNode.path("kind").textValue(),
                                List.of()
                        )
                );
            }
        });
        return messages;
    }

    private String translateRole(String role) {
        // If you think you need a change here, be sure to check the server as the translations must match. Once we have
        // all the SDKs and all customers on the new common format, this translation can go away.
        switch (role) {
            case "Human":
                return "user";
            case "Assistant":
                return "assistant";
            default:
                return role;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean isFormat(JsonNode templateNode, int version) {
        JsonNode formatVersionRaw = templateNode.path("format_version");
        if (formatVersionRaw.canConvertToInt()) {
            return formatVersionRaw.asInt() == version;
        } else {
            return false;
        }
    }
}
