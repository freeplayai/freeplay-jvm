package ai.freeplay.client.internal;

public class PromptUtils {
    public static String getFinalTag(String tag) {
        return tag != null ? tag : "latest";
    }
}
