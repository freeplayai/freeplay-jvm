package ai.freeplay.client.internal;

public class PromptUtils {
    public static String getFinalEnvironment(String environment) {
        return environment != null ? environment : "latest";
    }
}
