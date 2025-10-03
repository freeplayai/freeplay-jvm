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

public class OpenAILLMAdapter implements LLMAdapters.LLMAdapter<List<ChatMessage>> {
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
                if (part instanceof ContentPartText) {
                    return new ContentPart(((ContentPartText) part).getText());
                } else if (part instanceof ContentPartUrl) {
                    ContentPartUrl url = (ContentPartUrl) part;
                    if (url.getType() != MediaType.IMAGE) {
                        throw new IllegalStateException("Message contains a non-image URL, but OpenAI only supports image URLs.");
                    }
                    return new ContentPart(new ImageContent(url.getUrl()));
                } else if (part instanceof ContentPartBase64) {
                    return encodeBase64((ContentPartBase64) part);
                } else {
                    return part;
                }
            })).collect(toUnmodifiableList());

            return new ChatMessage(chatMessage.getRole(), openAIContent);
        }).collect(toUnmodifiableList());
    }

    private static Object encodeBase64(ContentPartBase64 part) {
        String base64Data = new String(part.getData());
        String contentFormat = part.getContentType().split("/")[1];
        if (part.getType() == MediaType.IMAGE) {
            return new ContentPart(new ImageContent(String.format("data:%s;base64,%s", part.getContentType(), base64Data)));
        } else if (part.getType() == MediaType.FILE) {
            return new ContentPart(new FileContent(
                    String.format("%s.%s", part.getSlotName(), contentFormat),
                    String.format("data:%s;base64,%s", part.getContentType(), base64Data)
            ));
        } else if (part.getType() == MediaType.AUDIO) {
            return new ContentPart(new AudioContent(base64Data, contentFormat.replaceFirst("mpeg", "mp3")));
        } else {
            return part;
        }
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
    public static class ContentPart {
        private String type;
        private AudioContent inputAudio;
        private ImageContent imageUrl;
        private FileContent file;
        private String text;

        public ContentPart(AudioContent inputAudio) {
            this.type = "input_audio";
            this.inputAudio = inputAudio;
        }

        public ContentPart(ImageContent imageUrl) {
            this.type = "image_url";
            this.imageUrl = imageUrl;
        }

        public ContentPart(FileContent file) {
            this.type = "file";
            this.file = file;
        }

        public ContentPart(String text) {
            this.type = "text";
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public AudioContent getInputAudio() {
            return inputAudio;
        }

        public ImageContent getImageUrl() {
            return imageUrl;
        }

        public FileContent getFile() {
            return file;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ContentPart that = (ContentPart) o;
            return Objects.equals(type, that.type) && Objects.equals(inputAudio, that.inputAudio) && Objects.equals(imageUrl, that.imageUrl) && Objects.equals(file, that.file) && Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, inputAudio, imageUrl, file, text);
        }

        @Override
        public String toString() {
            return "ContentPart{" +
                    "type='" + type + '\'' +
                    ", inputAudio=" + inputAudio +
                    ", imageUrl=" + imageUrl +
                    ", file=" + file +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AudioContent {
        private String data;
        private String format;

        public AudioContent(String data, String format) {
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
            AudioContent that = (AudioContent) o;
            return Objects.equals(data, that.data) && Objects.equals(format, that.format);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, format);
        }

        @Override
        public String toString() {
            return "AudioContent{" +
                    "data='" + data + '\'' +
                    ", format='" + format + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ImageContent {
        private String url;

        public ImageContent(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ImageContent that = (ImageContent) o;
            return Objects.equals(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(url);
        }

        @Override
        public String toString() {
            return "ImageContent{" +
                    "url='" + url + '\'' +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class FileContent {
        private String filename;
        private String fileData;

        public FileContent(String filename, String fileData) {
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
            FileContent that = (FileContent) o;
            return Objects.equals(filename, that.filename) && Objects.equals(fileData, that.fileData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename, fileData);
        }

        @Override
        public String toString() {
            return "FileContent{" +
                    "filename='" + filename + '\'' +
                    ", fileData='" + fileData + '\'' +
                    '}';
        }
    }
}
