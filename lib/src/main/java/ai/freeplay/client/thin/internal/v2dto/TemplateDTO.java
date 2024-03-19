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

    public TemplateDTO() {
    }

    public TemplateDTO(
            String promptTemplateId,
            String promptTemplateVersionId,
            String promptTemplateName,
            List<Message> content,
            Metadata metadata,
            int formatVersion
    ) {
        this.promptTemplateId = promptTemplateId;
        this.promptTemplateVersionId = promptTemplateVersionId;
        this.promptTemplateName = promptTemplateName;
        this.content = content;
        this.metadata = metadata;
        this.formatVersion = formatVersion;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateDTO that = (TemplateDTO) o;
        return formatVersion == that.formatVersion && Objects.equals(promptTemplateId, that.promptTemplateId) && Objects.equals(promptTemplateVersionId, that.promptTemplateVersionId) && Objects.equals(promptTemplateName, that.promptTemplateName) && Objects.equals(content, that.content) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptTemplateId, promptTemplateVersionId, promptTemplateName, content, metadata, formatVersion);
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
    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return Objects.equals(role, message.role) && Objects.equals(content, message.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, content);
        }

        @Override
        public String toString() {
            return "Message{" +
                    "role='" + role + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}
