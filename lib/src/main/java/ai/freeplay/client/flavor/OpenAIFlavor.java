package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class OpenAIFlavor<P, R> implements Flavor<P, R> {

    public String getProvider() {
        return "openai";
    }

    protected static void validateChoices(List<Map<String, Object>> choices) throws FreeplayException {
        if (choices.isEmpty()) {
            throw new FreeplayException("Did not get any 'choices' back from OpenAI.");
        }
    }

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

    protected Stream<String> callOpenAIStream(
            ProviderConfig providerConfig,
            String url,
            String promptFieldName,
            Map<String, Object> mergedLLMParameters,
            P formattedPrompt
    ) {
        validateParameters(mergedLLMParameters);

        Map<String, Object> bodyMap = new HashMap<>(mergedLLMParameters);
        bodyMap.put(promptFieldName, formattedPrompt);
        bodyMap.put("stream", true);

        HttpResponse<Stream<String>> response;
        try {
            response = Http.postJsonWithBearer(
                    url,
                    bodyMap,
                    providerConfig.getApiKey(),
                    HttpResponse.BodyHandlers.ofLines()
            );
        } catch (Exception e) {
            throw new FreeplayException("Error calling OpenAI.", e);
        }

        return response.body();
    }

    protected R parseLine(String line, Function<Map<String, Object>, R> itemCreator) {
        String[] field = line.split(":", 2);
        if (field.length == 2 && "data".equals(field[0])) {
            if ("[DONE]".equals(field[1].trim())) {
                return null;
            } else {
                Map<String, Object> objectMap = JSONUtil.parseMap(field[1]);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) objectMap.get("choices");
                Map<String, Object> firstChoice = choices.get(0);
                return itemCreator.apply(firstChoice);
            }
        } else {
            throw new FreeplayException("Got unknown line in the stream: '" + line + "'");
        }
    }
}
