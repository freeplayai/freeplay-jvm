package ai.freeplay.client.thin;

import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.resources.prompts.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

public class AnthropicLLMAdapter implements LLMAdapters.LLMAdapter<List<ChatMessage>> {
    @Override
    public String getProvider() {
        return "anthropic";
    }

    @Override
    public List<ChatMessage> toLLMSyntax(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> !message.getRole().equals("system"))
                .map(chatMessage -> {
                    if (!chatMessage.isStructuredMessage()) {
                        return chatMessage;
                    }
                    List<Object> content = chatMessage.getStructuredContent();

                    List<Object> anthropicContent = content.stream().map(part -> {
                        if (part instanceof ContentPartText) {
                            return new ContentPart<Void>(((ContentPartText) part).getText());
                        } else if (part instanceof ContentPartUrl) {
                            ContentPartUrl url = (ContentPartUrl) part;
                            return new ContentPart<>(anthropicType(url.getType()), new UrlContent(url.getUrl()));
                        } else if (part instanceof ContentPartBase64) {
                            ContentPartBase64 base64 = (ContentPartBase64) part;
                            return new ContentPart<>(anthropicType(base64.getType()), new Base64Content(
                                    base64.getContentType(),
                                    new String(base64.getData())
                            ));
                        } else {
                            return part;
                        }
                    }).collect(toUnmodifiableList());

                    return new ChatMessage(chatMessage.getRole(), anthropicContent);
                })
                .collect(toUnmodifiableList());
    }

    @Override
    public List<Map<String, Object>> toToolSchemaFormat(List<TemplateDTO.ToolSchema> toolSchema) {
        if (toolSchema == null) {
            return null;
        }

        return toolSchema.stream()
                .filter(schema -> schema.getName() != null
                        && schema.getDescription() != null
                        && schema.getParameters() != null)
                .map(schema -> Map.of(
                        "name", schema.getName(),
                        "description", schema.getDescription(),
                        "input_schema", schema.getParameters()
                ))
                .collect(toList());
    }

    private String anthropicType(MediaType mediaType) {
        if (mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO ) {
            throw new IllegalStateException("Anthropic does not support audio or video content");
        }

        if (mediaType == MediaType.IMAGE) {
            return "image";
        } else {
            return "document";
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentPart<T> {
        private String type;
        private String text;
        private T source;

        public ContentPart(String text) {
            this.type = "text";
            this.text = text;
        }

        public ContentPart(String type, T source) {
            this.type = type;
            this.source = source;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public T getSource() {
            return source;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ContentPart<?> that = (ContentPart<?>) o;
            return Objects.equals(type, that.type) && Objects.equals(text, that.text) && Objects.equals(source, that.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, text, source);
        }

        @Override
        public String toString() {
            return "ContentPart{" +
                    "type='" + type + '\'' +
                    ", text='" + text + '\'' +
                    ", content=" + source +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class UrlContent {
        private String type;
        private String url;

        public UrlContent(String url) {
            this.type = "url";
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            UrlContent that = (UrlContent) o;
            return Objects.equals(type, that.type) && Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, url);
        }

        @Override
        public String toString() {
            return "UrlContent{" +
                    "type='" + type + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Base64Content {
        private String type;
        private String mediaType;
        private String data;

        public Base64Content(String mediaType, String data) {
            this.type = "base64";
            this.mediaType = mediaType;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public String getMediaType() {
            return mediaType;
        }

        public String getData() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Base64Content that = (Base64Content) o;
            return Objects.equals(type, that.type) && Objects.equals(mediaType, that.mediaType) && Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, mediaType, data);
        }

        @Override
        public String toString() {
            return "Base64Content{" +
                    "type='" + type + '\'' +
                    ", mediaType='" + mediaType + '\'' +
                    ", data='" + data + '\'' +
                    '}';
        }
    }
}
