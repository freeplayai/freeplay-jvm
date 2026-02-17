package ai.freeplay.client.thin;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.prompts.ContentPartBase64;
import ai.freeplay.client.thin.resources.prompts.ContentPartText;
import ai.freeplay.client.thin.resources.prompts.ContentPartUrl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.vertexai.api.Blob;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.protobuf.ByteString;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class GeminiLLMAdapter implements LLMAdapters.LLMAdapter<List<Content>> {
    @Override
    public String getProvider() {
        return "vertex";
    }

    @Override
    public List<Content> toLLMSyntax(List<ChatMessage> messages) {
        try {
            return toLLMSyntaxInternal(messages);
        } catch (NoClassDefFoundError e) {
            throw new FreeplayConfigurationException(
                    "\nThe Google Cloud Vertex AI library is required for Gemini prompts but was not found on the classpath. " +
                            "This is an optional dependency of the Freeplay SDK. To fix this, either:\n" +
                            "  - Gradle (feature variant): Add the 'gemini' capability to your Freeplay dependency:\n" +
                            "      implementation(\"ai.freeplay:client\") { capabilities { requireCapability(\"ai.freeplay:client-gemini\") } }\n" +
                            "  - Gradle (direct): Add implementation(\"com.google.cloud:google-cloud-vertexai:1.5.0\")\n" +
                            "  - Maven: Add com.google.cloud:google-cloud-vertexai:1.5.0 to your dependencies\n" +
                            "**Replace the version number with the correct current version for google-cloud-vertex",
                    e
            );
        }
    }

    private List<Content> toLLMSyntaxInternal(List<ChatMessage> messages) {
        return messages
                .stream()
                .filter(message -> !message.getRole().equals("system"))
                .map(message -> {
                            if (message.isStringMessage()) {
                                return ContentMaker.forRole(translateRole(message.getRole())).fromString(message.getContent());
                            } else if (message.isStructuredMessage()) {
                                Object[] parts = message.getStructuredContent().stream().map(item -> {
                                    if (item instanceof ContentPartText) {
                                        return Part.newBuilder().setText(((ContentPartText) item).getText()).build();
                                    } else if (item instanceof ContentPartUrl) {
                                        throw new IllegalStateException("Message contains a media URL, but media URLs are not supported by Gemini");
                                    } else if (item instanceof ContentPartBase64) {
                                        ContentPartBase64 base64 = (ContentPartBase64) item;
                                        byte[] decodedBytes = Base64.getDecoder().decode(base64.getData());

                                        return Part.newBuilder().setInlineData(Blob.newBuilder()
                                                .setMimeType(base64.getContentType())
                                                .setData(ByteString.copyFrom(decodedBytes))
                                        ).build();
                                    } else {
                                        return Part.newBuilder().build();
                                    }
                                }).toArray();

                                return ContentMaker.forRole(translateRole(message.getRole())).fromMultiModalData(parts);
                            } else {
                                throw new FreeplayConfigurationException(format("Unknown message for Gemini: %s", message));
                            }
                        }
                )
                .collect(toList());
    }

    public static ChatMessage chatMessageFromContent(Content content) {
        List<Object> parts = content.getPartsList().stream().map(part -> {
            String text = part.getText();
            if (text != null && !text.isEmpty()) {
                return new ContentPart(text);
            }
            byte[] data = part.getInlineData().getData().toByteArray();
            String contentType = part.getInlineData().getMimeType();

            return new ContentPart(new InlineDataContent(contentType, Base64.getEncoder().encodeToString(data)));
        }).collect(toList());

        return ChatMessage.newForGemini(content.getRole(), parts);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentPart {
        private String text;
        private InlineDataContent inlineData;

        public ContentPart(String text) {
            this.text = text;
        }

        public ContentPart(InlineDataContent inlineData) {
            this.inlineData = inlineData;
        }

        public String getText() {
            return text;
        }

        public InlineDataContent getInlineData() {
            return inlineData;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ContentPart that = (ContentPart) o;
            return Objects.equals(text, that.text) && Objects.equals(inlineData, that.inlineData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, inlineData);
        }

        @Override
        public String toString() {
            return "ContentPart{" +
                    "text='" + text + '\'' +
                    ", inlineData=" + inlineData +
                    '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class InlineDataContent {
        private final String mimeType;
        private final String data;

        public InlineDataContent(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getData() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            InlineDataContent that = (InlineDataContent) o;
            return Objects.equals(mimeType, that.mimeType) && Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mimeType, data);
        }

        @Override
        public String toString() {
            return "InlineDataContent{" +
                    "mimeType='" + mimeType + '\'' +
                    ", data='" + data + '\'' +
                    '}';
        }
    }

    private String translateRole(String role) {
        switch (role) {
            case "user":
                return "user";
            case "assistant":
            case "model":
                return "model";
            default:
                throw new FreeplayConfigurationException(
                        format("Unknown role in prompt template for Gemini: %s", role)
                );
        }
    }
}
