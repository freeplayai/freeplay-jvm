package ai.freeplay.client.adapters;

import ai.freeplay.client.internal.v2dto.TemplateDTO;
import ai.freeplay.client.resources.prompts.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

public class OpenAILLMAdapter implements LLMAdapters.LLMAdapter<List<ChatMessage>> {
    @Override
    public RoleSupport getRoleSupport() {
        return RoleSupport.OPENAI;
    }

    @Override
    public String getProvider() {
        return "openai";
    }

    @Override
    public List<ChatMessage> toLLMSyntax(List<ChatMessage> messages) {
        return messages.stream().map(chatMessage -> {
            if (!chatMessage.isStructuredMessage()) {
                return chatMessage;
            }
            List<Object> content = chatMessage.getStructuredContent();

            List<Object> openAIContent = content.stream().map((part -> {
                if (part instanceof TextContent) {
                    return new OpenAIContentPart(((TextContent) part).getText());
                } else if (part instanceof ImageUrlContent) {
                    return new OpenAIContentPart(new OpenAIImageContent(((ImageUrlContent) part).getUrl()));
                } else if (part instanceof ImageContent) {
                    ImageContent img = (ImageContent) part;
                    return new OpenAIContentPart(new OpenAIImageContent(String.format("data:%s;base64,%s", img.getContentType(), img.getData())));
                } else if (part instanceof AudioContent) {
                    AudioContent audio = (AudioContent) part;
                    String format = audio.getContentType().split("/")[1].replaceFirst("mpeg", "mp3");
                    return new OpenAIContentPart(new OpenAIAudioContent(audio.getData(), format));
                } else if (part instanceof FileContent) {
                    FileContent file = (FileContent) part;
                    String contentFormat = file.getContentType().split("/")[1];
                    return new OpenAIContentPart(new OpenAIFileContent(
                            String.format("%s.%s", file.getFilename(), contentFormat),
                            String.format("data:%s;base64,%s", file.getContentType(), file.getData())
                    ));
                } else {
                    return part;
                }
            })).collect(toUnmodifiableList());

            return new ChatMessage(chatMessage.getRole(), openAIContent);
        }).collect(toUnmodifiableList());
    }

    @Override
    public List<Map<String, Object>> toToolSchemaFormat(List<TemplateDTO.ToolSchema> toolSchema) {
        if (toolSchema == null) {
            return null;
        }

        return toolSchema.stream()
                .map(schema -> Map.of(
                        "function", schema,
                        "type", "function"
                ))
                .collect(toList());
    }

    @Override
    public Map<String, Object> toOutputSchemaFormat(Map<String, Object> outputSchema) {
        // For OpenAI, the normalized format is compatible with the API format
        return outputSchema;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OpenAIContentPart {
        private String type;
        private OpenAIAudioContent inputAudio;
        private OpenAIImageContent imageUrl;
        private OpenAIFileContent file;
        private String text;

        public OpenAIContentPart(OpenAIAudioContent inputAudio) {
            this.type = "input_audio";
            this.inputAudio = inputAudio;
        }

        public OpenAIContentPart(OpenAIImageContent imageUrl) {
            this.type = "image_url";
            this.imageUrl = imageUrl;
        }

        public OpenAIContentPart(OpenAIFileContent file) {
            this.type = "file";
            this.file = file;
        }

        public OpenAIContentPart(String text) {
            this.type = "text";
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public OpenAIAudioContent getInputAudio() {
            return inputAudio;
        }

        public OpenAIImageContent getImageUrl() {
            return imageUrl;
        }

        public OpenAIFileContent getFile() {
            return file;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            OpenAIContentPart that = (OpenAIContentPart) o;
            return Objects.equals(type, that.type) && Objects.equals(inputAudio, that.inputAudio) && Objects.equals(imageUrl, that.imageUrl) && Objects.equals(file, that.file) && Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, inputAudio, imageUrl, file, text);
        }

        @Override
        public String toString() {
            return "OpenAIContentPart{" +
                    "type='" + type + '\'' +
                    ", inputAudio=" + inputAudio +
                    ", imageUrl=" + imageUrl +
                    ", file=" + file +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OpenAIAudioContent {
        private String data;
        private String format;

        public OpenAIAudioContent(String data, String format) {
            this.data = data;
            this.format = format;
        }

        public String getData() {
            return data;
        }

        public String getFormat() {
            return format;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            OpenAIAudioContent that = (OpenAIAudioContent) o;
            return Objects.equals(data, that.data) && Objects.equals(format, that.format);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, format);
        }

        @Override
        public String toString() {
            return "OpenAIAudioContent{" +
                    "data='" + data + '\'' +
                    ", format='" + format + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OpenAIImageContent {
        private String url;

        public OpenAIImageContent(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            OpenAIImageContent that = (OpenAIImageContent) o;
            return Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(url);
        }

        @Override
        public String toString() {
            return "OpenAIImageContent{" +
                    "url='" + url + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OpenAIFileContent {
        private String filename;
        private String fileData;

        public OpenAIFileContent(String filename, String fileData) {
            this.filename = filename;
            this.fileData = fileData;
        }

        public String getFilename() {
            return filename;
        }

        public String getFileData() {
            return fileData;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            OpenAIFileContent that = (OpenAIFileContent) o;
            return Objects.equals(filename, that.filename) && Objects.equals(fileData, that.fileData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename, fileData);
        }

        @Override
        public String toString() {
            return "OpenAIFileContent{" +
                    "filename='" + filename + '\'' +
                    ", fileData='" + fileData + '\'' +
                    '}';
        }
    }
}
