package ai.freeplay.client.flavor;

import ai.freeplay.client.ProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.HttpUtil;
import ai.freeplay.client.internal.TemplateUtils;
import ai.freeplay.client.model.CompletionResponse;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ai.freeplay.client.internal.HttpUtil.parseBody;
import static ai.freeplay.client.internal.HttpUtil.throwIfError;

public class OpenAITextFlavor extends OpenAIFlavor implements Flavor<String> {

    private static final String OPENAI_COMPLETIONS_URL = "https://api.openai.com/v1/completions";

    @Override
    public String formatPrompt(
            String template,
            Map<String, Object> variables
    ) {
        return TemplateUtils.format(template, variables);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionResponse callService(
            String formattedPrompt,
            ProviderConfig providerConfig,
            Map<String, Object> llmParameters
    ) throws FreeplayException {
        validateParameters(llmParameters);

        Map<String, Object> bodyMap = new HashMap<>(llmParameters);
        bodyMap.put("prompt", formattedPrompt);

        HttpResponse<String> response;
        try {
            response = HttpUtil.postJsonWithBearer(OPENAI_COMPLETIONS_URL, bodyMap, providerConfig.getApiKey());
        } catch (Exception e) {
            throw new FreeplayException("Error calling OpenAI.", e);
        }

        Map<String, Object> responseBody = parseBody(response);
        throwIfError(response, 200);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        validateChoices(choices);
        Map<String, Object> choice = choices.get(0);

        boolean isComplete = "stop".equals(choice.get("finish_reason"));
        return new CompletionResponse(String.valueOf(choice.get("text")), isComplete);
    }

    @Override
    public String getFormatType() {
        return "openai_text";
    }

    @Override
    public String serializeForRecord(String formattedPrompt) {
        return formattedPrompt;
    }

    private static void validateChoices(List<Map<String, Object>> choices) throws FreeplayException {
        if (choices.isEmpty()) {
            throw new FreeplayException("Did not get any 'choices' back from OpenAI.");
        }
    }
}
