package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.Http;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

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

    protected static <P, R> Stream<R> callOpenAIStream(
            ProviderConfig providerConfig,
            String url,
            String promptFieldName,
            Map<String, Object> mergedLLMParameters,
            P formattedPrompt,
            Function<Map<String, Object>, R> itemCreator
    ) {
        validateParameters(mergedLLMParameters);

        Map<String, Object> bodyMap = new HashMap<>(mergedLLMParameters);
        bodyMap.put(promptFieldName, formattedPrompt);
        bodyMap.put("stream", true);

        HttpResponse<Stream<R>> response;
        try {
            response = Http.postJsonWithBearer(
                    url,
                    bodyMap,
                    providerConfig.getApiKey(),
                    Http.ResponseHandlers.streamHandler(itemCreator)
            );
        } catch (Exception e) {
            throw new FreeplayException("Error calling OpenAI.", e);
        }

        return response.body();
    }

    public String getProvider() {
        return "openai";
    }
}
