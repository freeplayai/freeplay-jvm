package ai.freeplay.client.thin;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO;
import ai.freeplay.client.thin.internal.v2dto.TemplatesDTO;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.Http.throwFreeplayIfError;
import static java.lang.String.format;

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
    public CompletableFuture<TemplatesDTO> getPrompts(String projectId, String environment) {
        String url = format("%s/v2/projects/%s/prompt-templates/all/%s", baseUrl, projectId, environment);
        return AsyncHttp
                .get(url, freeplayApiKey, httpConfig)
                .thenApply((HttpResponse<String> response) -> {
                    throwFreeplayIfError(response, 200);
                    return JSONUtil.parse(response.body(), TemplatesDTO.class);
                });
    }

    @Override
    public CompletableFuture<TemplateDTO> getPrompt(String projectId, String templateName, String environment) {
        String encodedName = URLEncoder.encode(templateName, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedEnv = URLEncoder.encode(environment, StandardCharsets.UTF_8).replace("+", "%20");
        String url = format("%s/v2/projects/%s/prompt-templates/name/%s?environment=%s",
                baseUrl, projectId, encodedName, encodedEnv
        );
        return AsyncHttp
                .get(url, freeplayApiKey, httpConfig)
                .thenApply((HttpResponse<String> response) -> {
                    throwFreeplayIfError(response, 200);
                    return JSONUtil.parse(response.body(), TemplateDTO.class);
                });
    }

    @Override
    public CompletableFuture<TemplateDTO> getPromptByVersionId(String projectId, String templateId, String templateVersionId) {
        String url = format("%s/v2/projects/%s/prompt-templates/id/%s/versions/%s",
                baseUrl, projectId, templateId, templateVersionId
        );
        return AsyncHttp
                .get(url, freeplayApiKey, httpConfig)
                .thenApply((HttpResponse<String> response) -> {
                    throwFreeplayIfError(response, 200);
                    return JSONUtil.parse(response.body(), TemplateDTO.class);
                });
    }
}
