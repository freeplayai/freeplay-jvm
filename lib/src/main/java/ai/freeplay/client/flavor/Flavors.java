package ai.freeplay.client.flavor;

import ai.freeplay.client.exceptions.FreeplayConfigurationException;

import static java.lang.String.format;

public class Flavors {
    public static ChatFlavor getFlavorByName(String flavorName) {
        switch (flavorName) {
            case "openai_chat":
                return new OpenAIChatFlavor();
            case "anthropic_chat":
                return new AnthropicChatFlavor();
            default:
                throw new FreeplayConfigurationException(format("Unable to create Flavor for name '%s'.%n", flavorName));
        }
    }
}
