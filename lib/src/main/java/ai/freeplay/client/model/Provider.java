package ai.freeplay.client.model;

public enum Provider {
    OpenAI("openai", "OpenAI"),
    Anthropic("anthropic", "Anthropic");

    private final String name;
    private final String friendlyName;

    Provider(String name, String friendlyName) {
        this.name = name;
        this.friendlyName = friendlyName;
    }

    public String getName() {
        return name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }
}
