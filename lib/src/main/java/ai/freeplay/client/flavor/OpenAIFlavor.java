package ai.freeplay.client.flavor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import ai.freeplay.client.ProviderConfigs;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.LLMClientException;
import ai.freeplay.client.exceptions.LLMServerException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.Provider;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class OpenAIFlavor<P, R> implements Flavor<P, R> {

    @Override
    public Provider getProviderEnum() {
        return Provider.OpenAI;
    }

    protected static void validateChoices(List<Map<String, Object>> choices) throws FreeplayException {
        if (choices.isEmpty()) {
            throw new LLMServerException("Did not get any 'choices' back from OpenAI.");
        }
    }

    protected static void validateParameters(Map<String, Object> llmParameters) {
        if (!llmParameters.containsKey("model")) {
            throw new LLMClientException("The 'model' parameter is required when calling OpenAI");
        }
        if (llmParameters.containsKey("prompt")) {
            throw new LLMClientException("The 'prompt' parameter cannot be specified. It is populated automatically.");
        }
        if (llmParameters.containsKey("messages")) {
            throw new LLMClientException("The 'messages' parameter cannot be specified. It is populated automatically.");
        }
    }

    protected Stream<String> callOpenAIStream(
            ProviderConfigs providerConfig,
            String url,
            String promptFieldName,
            Map<String, Object> mergedLLMParameters,
            P formattedPrompt,
            HttpConfig httpConfig
    ) {
        validateParameters(mergedLLMParameters);
        OpenAIProviderConfig openAIProviderConfig = validateConfig(providerConfig);

        Map<String, Object> bodyMap = new HashMap<>(mergedLLMParameters);
        bodyMap.put(promptFieldName, formattedPrompt);
        bodyMap.put("stream", true);

        HttpResponse<Stream<String>> response;
        try {
            response = Http.postJsonWithBearer(
                    url,
                    bodyMap,
                    openAIProviderConfig.getApiKey(),
                    HttpResponse.BodyHandlers.ofLines(),
                    httpConfig
            );
        } catch (Exception e) {
            throw new LLMServerException("Error calling OpenAI.", e);
        }

        return response.body();
    }

    protected R parseLine(String line, Function<Map<String, Object>, R> itemCreator) {
        String[] field = line.split(":", 2);
        if (field.length == 2 && "data".equals(field[0])) {
            if ("[DONE]".equals(field[1].trim())) {
                return null;
            } else {
                Map<String, Object> objectMap;
                try {
                    objectMap = JSONUtil.parseMap(field[1]);
                } catch (Exception e) {
                    throw new LLMServerException("Error processing OpenAI stream.", e);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) objectMap.get("choices");
                Map<String, Object> firstChoice = choices.get(0);
                return itemCreator.apply(firstChoice);
            }
        } else {
            throw new LLMServerException("Got unknown line in the stream: '" + line + "'");
        }
    }

    protected OpenAIProviderConfig validateConfig(ProviderConfigs providerConfig) {
        if (providerConfig.getOpenAIConfig() != null) {
            return providerConfig.getOpenAIConfig();
        } else {
            throw new FreeplayConfigurationException("The OpenAI provider is not configured on the ProviderConfig. " +
                    "Set up this provider config to call OpenAI endpoints.");
        }
    }
}
