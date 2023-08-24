package ai.freeplay.client.flavor;

import ai.freeplay.client.exceptions.FreeplayException;

import java.util.Map;

public class OpenAIFlavor {
    protected static void validateParameters(Map<String, Object> llmParameters) {
        if (!llmParameters.containsKey("model")) {
            throw new FreeplayException("The 'model' parameter is required when calling OpenAI");
        }
        if (llmParameters.containsKey("prompt")) {
            throw new FreeplayException("The 'prompt' parameter cannot be specified. It is populated automatically.");
        }
        if (llmParameters.containsKey("messages")) {
            throw new FreeplayException("The 'messages' parameter cannot be specified. It is populated automatically.");
        }
    }

    public String getProvider() {
        return "openai";
    }
}
