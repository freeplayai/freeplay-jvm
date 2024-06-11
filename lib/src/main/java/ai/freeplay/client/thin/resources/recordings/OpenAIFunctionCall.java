package ai.freeplay.client.thin.resources.recordings;

import java.util.Map;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class OpenAIFunctionCall {
    private final String name;
    private final String arguments;

    public OpenAIFunctionCall(String name, String arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }
    public String getArguments() {
        return arguments;
    }

    public Map<String, String> asMap() {
        return Map.of(
                "name", name,
                "arguments", arguments
        );
    }

    public String toString() {
        return "OpenAIFunctionCall(name=" + this.name + ", arguments=" + this.arguments + ")";
    }
}
