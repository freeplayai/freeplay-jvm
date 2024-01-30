package ai.freeplay.client.thin;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.internal.AsyncHttp;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.internal.dto.TemplatesDTO;

import java.net.http.HttpResponse;
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
        String url = format("%s/projects/%s/templates/all/%s", baseUrl, projectId, environment);
        return AsyncHttp
                .get(url, freeplayApiKey, httpConfig)
                .thenApply((HttpResponse<String> response) -> {
                    throwFreeplayIfError(response, 200);
                    return JSONUtil.parse(response.body(), TemplatesDTO.class);
                });
    }
}
