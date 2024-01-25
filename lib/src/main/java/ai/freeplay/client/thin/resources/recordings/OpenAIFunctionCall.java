package ai.freeplay.client.thin.resources.recordings;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class OpenAIFunctionCall {
    private final String name;
    private final String arguments;

    public OpenAIFunctionCall(String name, String arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}
