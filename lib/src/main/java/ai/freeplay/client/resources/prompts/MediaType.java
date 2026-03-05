package ai.freeplay.client.resources.prompts;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MediaType {
    @JsonProperty("image")
    IMAGE,
    @JsonProperty("audio")
    AUDIO,
    @JsonProperty("file")
    FILE,
    @JsonProperty("video")
    VIDEO
}
