package ai.freeplay.client.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.FreeplayException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class AsyncHttp {

    public static CompletableFuture<HttpResponse<String>> get(
            String url,
            String apiKey,
            HttpConfig httpConfig
    ) throws FreeplayException {
        HttpRequest.Builder requestBuilder = request(url, apiKey, httpConfig);

        try {
            return client(httpConfig)
                    .build()
                    .sendAsync(requestBuilder.build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new FreeplayException("Error sending GET request.", e);
        }
    }

    public static CompletableFuture<HttpResponse<String>> postJson(
            String url,
            String apiKey,
            HttpConfig httpConfig,
            Object body
    ) throws FreeplayException {
        return sendJson(url, apiKey, httpConfig, "POST", body);
    }

    public static CompletableFuture<HttpResponse<String>> putJson(
            String url,
            String apiKey,
            HttpConfig httpConfig,
            Object body
    ) throws FreeplayException {
        return sendJson(url, apiKey, httpConfig, "PUT", body);
    }

    public static CompletableFuture<HttpResponse<String>> patchJson(
            String url,
            String apiKey,
            HttpConfig httpConfig,
            Object body
    ) throws FreeplayException {
        return sendJson(url, apiKey, httpConfig, "PATCH", body);
    }

    public static CompletableFuture<HttpResponse<String>> delete(
            String url,
            String apiKey,
            HttpConfig httpConfig
    ) throws FreeplayException {
        HttpRequest.Builder requestBuilder = request(url, apiKey, httpConfig);

        try {
            return client(httpConfig)
                    .build()
                    .sendAsync(requestBuilder.DELETE().build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new FreeplayException("Error sending DELETE request.", e);
        }
    }

    private static CompletableFuture<HttpResponse<String>> sendJson(
            String url,
            String apiKey,
            HttpConfig httpConfig,
            String method,
            Object body
    ) throws FreeplayException {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(JSONUtil.toString(body));
        HttpRequest.Builder request =
                request(url, apiKey, httpConfig)
                        .method(method, bodyPublisher)
                        .header("Content-Type", "application/json");

        try {
            return client(httpConfig)
                    .build()
                    .sendAsync(request.build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new FreeplayException(format("Error sending %s request.", method), e);
        }
    }

    private static HttpClient.Builder client(HttpConfig httpConfig) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        if (httpConfig.getExecutor() != null) {
            clientBuilder.executor(httpConfig.getExecutor());
        }
        if (httpConfig.getProxySelector() != null) {
            clientBuilder.proxy(httpConfig.getProxySelector());
        }
        return clientBuilder;
    }

    private static HttpRequest.Builder request(
            String url,
            String apiKey,
            HttpConfig httpConfig
    ) {
        HttpRequest.Builder requestBuilder;
        try {
            requestBuilder = HttpRequest
                    .newBuilder(new URI(url))
                    .header("User-Agent", UserAgent.getUserAgent());

            if (apiKey != null) {
                requestBuilder.header("Authorization", format("Bearer %s", apiKey));
            }

            if (httpConfig.getRequestTimeout() != null) {
                requestBuilder.timeout(httpConfig.getRequestTimeout());
            }
        } catch (URISyntaxException e) {
            throw new FreeplayException("Error in URL during GET request.", e);
        }
        return requestBuilder;
    }
}
