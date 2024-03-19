package ai.freeplay.client.processor;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.exceptions.FreeplayServerException;
import ai.freeplay.client.internal.Http;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.PromptTemplate;

import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

public class APITemplateResolver implements TemplateResolver {
    private final String baseUrl;
    private final String freeplayApiKey;
    private final HttpConfig httpConfig;

    public APITemplateResolver(String baseUrl, String freeplayApiKey, HttpConfig httpConfig) {
        this.baseUrl = baseUrl;
        this.freeplayApiKey = freeplayApiKey;
        this.httpConfig = httpConfig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<PromptTemplate> getPrompts(String projectId, String environment) {
        String url = format("%s/v2/projects/%s/prompt-templates/all/%s", baseUrl, projectId, environment);
        HttpResponse<String> response = Http.get(url, freeplayApiKey, httpConfig);
        throwFreeplayIfError(response, 200);

        Map<String, Object> templatesMap;
        try {
            templatesMap = Http.parseBody(response);
        } catch (FreeplayException e) {
            throw new FreeplayServerException("Error getting prompts.", e);
        }
        List<Map<String, Object>> templates = (List<Map<String, Object>>) templatesMap.get("prompt_templates");
        return templates.stream().map((Object template) -> {
            Map<String, Object> templateMap = (Map<String, Object>) template;
            Map<String, Object> metadataMap = (Map<String, Object>) templateMap.get("metadata");
            Map<String, Object> paramsMap = (Map<String, Object>) metadataMap.get("params");
            paramsMap.put("model", metadataMap.get("model"));
            return new PromptTemplate(
                    valueOf(templateMap.get("prompt_template_name")),
                    valueOf(JSONUtil.toString(templateMap.get("content"))),
                    valueOf(metadataMap.get("flavor")),
                    valueOf(templateMap.get("prompt_template_version_id")),
                    valueOf(templateMap.get("prompt_template_id")),
                    valueOf(templateMap.get("prompt_template_version_id")),
                    paramsMap
            );
        }).collect(toList());
    }
}
