package ai.freeplay.client.thin.internal.v2dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TemplateDTO {
    private String promptTemplateId;
    private String promptTemplateVersionId;
    private String promptTemplateName;

    private List<Message> content;
    private Metadata metadata;

    private int formatVersion = -1;
    private String projectId;
    private List<ToolSchema> toolSchema;
    private Map<String, Object> outputSchema;

    public TemplateDTO() {
    }
    
    public TemplateDTO(
            String promptTemplateId,
            String promptTemplateVersionId,
            String promptTemplateName,
            List<Message> content,
            Metadata metadata,
            int formatVersion,
            String projectId
    ) {
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.promptTemplateName = promptTemplateName;
        this.content = content;
        this.metadata = metadata;
        this.formatVersion = formatVersion;
        this.projectId = projectId;
        this.toolSchema = null;
    }

    public TemplateDTO(
            String promptTemplateId,
            String promptTemplateVersionId,
            String promptTemplateName,
            List<Message> content,
            Metadata metadata,
            int formatVersion,
            String projectId,
            List<ToolSchema> toolSchema
    ) {
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.promptTemplateName = promptTemplateName;
        this.content = content;
        this.metadata = metadata;
        this.formatVersion = formatVersion;
        this.projectId = projectId;
        this.toolSchema = toolSchema;
        this.outputSchema = null;
    }

    public String getPromptTemplateId() {
        return promptTemplateId;
    }

    public String getPromptTemplateVersionId() {
        return promptTemplateVersionId;
    }

    public String getPromptTemplateName() {
        return promptTemplateName;
    }

    public List<Message> getContent() {
        return content;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public String getProjectId() {
        return projectId;
    }

    public List<ToolSchema> getToolSchema() {
        return toolSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateDTO that = (TemplateDTO) o;
        return formatVersion == that.formatVersion && Objects.equals(promptTemplateId, that.promptTemplateId) && Objects.equals(promptTemplateVersionId, that.promptTemplateVersionId) && Objects.equals(promptTemplateName, that.promptTemplateName) && Objects.equals(content, that.content) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptTemplateId, promptTemplateVersionId, promptTemplateName, content, metadata, formatVersion, projectId);
    }

    @Override
    public String toString() {
        return "TemplateDTO{" +
                "promptTemplateId='" + promptTemplateId + '\'' +
                ", promptTemplateVersionId='" + promptTemplateVersionId + '\'' +
                ", promptTemplateName='" + promptTemplateName + '\'' +
                ", content=" + content +
                ", metadata=" + metadata +
                ", formatVersion=" + formatVersion +
                ", projectId='" + projectId + '\'' +
                '}';
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Metadata {
        private String provider;
        private String model;
        private String flavor;
        private Map<String, Object> params;
        private Map<String, Object> providerInfo;

        public Metadata() {
        }

        public Metadata(
                String provider,
                String model,
                String flavor,
                Map<String, Object> params,
                Map<String, Object> providerInfo
        ) {
            this.provider = provider;
            this.model = model;
            this.flavor = flavor;
            this.params = params;
            this.providerInfo = providerInfo;
        }

        public String getProvider() {
            return provider;
        }

        public String getModel() {
            return model;
        }

        public String getFlavor() {
            return flavor;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public Map<String, Object> getProviderInfo() {
            return providerInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Metadata metadata = (Metadata) o;
            return Objects.equals(provider, metadata.provider) && Objects.equals(model, metadata.model) && Objects.equals(flavor, metadata.flavor) && Objects.equals(params, metadata.params) && Objects.equals(providerInfo, metadata.providerInfo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, model, flavor, params, providerInfo);
        }

        @Override
        public String toString() {
            return "Metadata{" +
                    "provider='" + provider + '\'' +
                    ", model='" + model + '\'' +
                    ", flavor='" + flavor + '\'' +
                    ", params=" + params +
                    ", providerInfo=" + providerInfo +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MediaSlot {
        String type;
        String placeholderName;

        public MediaSlot() {
        }

        public MediaSlot(String type, String placeholderName) {
            this.type = type;
            this.placeholderName = placeholderName;
        }

        public String getType() {
            return type;
        }

        public String getPlaceholderName() {
            return placeholderName;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            MediaSlot mediaSlot = (MediaSlot) o;
            return Objects.equals(type, mediaSlot.type) && Objects.equals(placeholderName, mediaSlot.placeholderName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, placeholderName);
        }

        @Override
        public String toString() {
            return "MediaSlot{" +
                    "type='" + type + '\'' +
                    ", placeholderName='" + placeholderName + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Message {
        private String role;
        private String content;
        private String kind;
        private List<MediaSlot> mediaSlots;

        public Message() {
        }

        public Message(String role, String content, String kind) {
            this.role = role;
            this.content = content;
            this.kind = kind;
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public Message(String role, String content, String kind, List<MediaSlot> mediaSlots) {
            this.role = role;
            this.content = content;
            this.kind = kind;
            this.mediaSlots = mediaSlots;
        }

        public Message(String role, String content, List<MediaSlot> mediaSlots) {
            this.role = role;
            this.content = content;
            this.mediaSlots = mediaSlots;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public boolean isKind() {
            return kind != null;
        }

        public String getKind() {
            return kind;
        }

        public List<MediaSlot> getMediaSlots() {
            return mediaSlots;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return Objects.equals(role, message.role) && Objects.equals(content, message.content) && Objects.equals(kind, message.kind) && Objects.equals(mediaSlots, message.mediaSlots);
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, content, kind, mediaSlots);
        }

        @Override
        public String toString() {
            return "Message{" +
                    "role='" + role + '\'' +
                    ", content='" + content + '\'' +
                    ", kind='" + kind + '\'' +
                    ", mediaSlots=" + mediaSlots +
                    '}';
        }
    }

    // This class is actually exposed in the public API of the SDK, though DTO classes are normally
    // considered internal. If you change this class, consider if it's a breaking change for customers.
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ToolSchema {
        private String description;
        private String name;
        private Map<String, Object> parameters;

        public ToolSchema() {
        }

        public ToolSchema(String name, String description,Map<String, Object> parameters) {
            this.description = description;
            this.name = name;
            this.parameters = parameters;
        }

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ToolSchema)) return false;
            ToolSchema that = (ToolSchema) o;
            return Objects.equals(description, that.description) &&
                   Objects.equals(name, that.name) &&
                   Objects.equals(parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, name, parameters);
        }

        @Override
        public String toString() {
            return "ToolSchema{" +
                    "description='" + description + '\'' +
                    ", name='" + name + '\'' +
                    ", parameters=" + parameters +
                    '}';
        }
    }
}
