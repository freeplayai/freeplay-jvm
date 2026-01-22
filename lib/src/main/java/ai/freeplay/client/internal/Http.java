package ai.freeplay.client.internal;

import ai.freeplay.client.HttpConfig;
import ai.freeplay.client.exceptions.*;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.String.format;

public class Http {

    public static HttpResponse<String> postWithBearer(String url, String apiKey, HttpConfig httpConfig) throws FreeplayException {
        return postJsonWithBearer(url, null, apiKey, BodyHandlers.ofString(), httpConfig);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static HttpResponse<String> postJsonWithBearer(
            String url,
            Map<String, Object> body,
            String apiKey
    ) throws FreeplayException {
        String jsonString = JSONUtil.asString(body);
        return postJsonWithBearer(url, jsonString, apiKey, BodyHandlers.ofString(), new HttpConfig());
    }

    public static HttpResponse<String> postJsonWithBearer(
            String url,
            Map<String, Object> body,
            String apiKey,
            HttpConfig httpConfig
    ) throws FreeplayException {
        String jsonString = JSONUtil.asString(body);
        return postJsonWithBearer(url, jsonString, apiKey, BodyHandlers.ofString(), httpConfig);
    }

    public static <R> HttpResponse<R> postJsonWithBearer(
            String url,
            Map<String, Object> body,
            String apiKey,
            BodyHandler<R> responseBodyHandler,
            HttpConfig httpConfig
    ) throws FreeplayException {
        return postJsonWithBearer(url, JSONUtil.asString(body), apiKey, responseBodyHandler, httpConfig);
    }

    public static <R> HttpResponse<R> postJsonWithBearer(
            String url,
            String body,
            String apiKey,
            BodyHandler<R> responseBodyHandler,
            HttpConfig httpConfig,
            String... headers
    ) throws FreeplayException {
        List<String> allHeadersArray = new ArrayList<>(headers.length + 3);
        Collections.addAll(allHeadersArray, headers);
        Collections.addAll(allHeadersArray, authHeaders(apiKey));
        String[] allHeaders = allHeadersArray.toArray(new String[]{});

        return postJson(url, body, responseBodyHandler, httpConfig, allHeaders);
    }

    public static String[] authHeaders(String apiKey) {
        return new String[]{"Authorization", format("Bearer %s", apiKey)};
    }

    public static HttpResponse<String> postJson(
            String url,
            Map<String, Object> body,
            HttpConfig httpConfig,
            String... headers
    ) throws FreeplayException {
        return postJson(url, body, BodyHandlers.ofString(), httpConfig, headers);
    }

    public static <R> HttpResponse<R> postJson(
            String url,
            Map<String, Object> body,
            BodyHandler<R> responseBodyHandler,
            HttpConfig httpConfig,
            String... headers
    ) throws FreeplayException {
        String jsonString = JSONUtil.asString(body);
        return postJson(url, jsonString, responseBodyHandler, httpConfig, headers);
    }

    public static <R> HttpResponse<R> postJson(
            String url,
            String body,
            BodyHandler<R> responseBodyHandler,
            HttpConfig httpConfig,
            String... headers
    ) throws FreeplayException {
        return jsonRequest(url, body, responseBodyHandler, httpConfig, "POST", headers);
    }

    public static <R> HttpResponse<R> jsonRequest(
            String url,
            String body,
            BodyHandler<R> responseBodyHandler,
            HttpConfig httpConfig,
            String method,
            String... headers
    ) throws FreeplayException {
        HttpRequest.BodyPublisher bodyPublisher = body != null ?
                HttpRequest.BodyPublishers.ofString(body) :
                HttpRequest.BodyPublishers.noBody();
        HttpRequest.Builder requestBuilder;
        try {
            requestBuilder = HttpRequest
                    .newBuilder(new URI(url))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", UserAgent.getUserAgent())
                    .headers(headers)
                    .method(method, bodyPublisher);
            if (httpConfig.getRequestTimeout() != null) {
                requestBuilder.timeout(httpConfig.getRequestTimeout());
            }

            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (httpConfig.getExecutor() != null) {
                clientBuilder.executor(httpConfig.getExecutor());
            }
            if (httpConfig.getProxySelector() != null) {
                clientBuilder.proxy(httpConfig.getProxySelector());
            }
            return clientBuilder
                    .build()
                    .send(requestBuilder.build(), responseBodyHandler);
        } catch (Exception e) {
            throw new FreeplayException("Error sending POST request.", e);
        }
    }

    public static HttpResponse<String> get(String url, String apiKey, HttpConfig httpConfig) throws FreeplayException {
        HttpRequest.Builder requestBuilder;
        try {
            requestBuilder = HttpRequest.newBuilder(new URI(url));
        } catch (URISyntaxException e) {
            throw new FreeplayException("Error in URL during GET request.", e);
        }

        if (apiKey != null) {
            requestBuilder.header("Authorization", format("Bearer %s", apiKey));
        }

        requestBuilder.header("User-Agent", UserAgent.getUserAgent());

        if (httpConfig.getRequestTimeout() != null) {
            requestBuilder.timeout(httpConfig.getRequestTimeout());
        }

        try {
            HttpClient.Builder clientBuilder = HttpClient.newBuilder();
            if (httpConfig.getExecutor() != null) {
                clientBuilder.executor(httpConfig.getExecutor());
            }
            if (httpConfig.getProxySelector() != null) {
                clientBuilder.proxy(httpConfig.getProxySelector());
            }
            return clientBuilder
                    .build()
                    .send(requestBuilder.build(), BodyHandlers.ofString());
        } catch (Exception e) {
            throw new FreeplayException("Error sending GET request.", e);
        }
    }

    public static Map<String, Object> parseBody(HttpResponse<String> response) throws FreeplayException {
        try {
            return JSON.std.mapFrom(response.body());
        } catch (IOException e) {
            throw new FreeplayException("Unable to parse JSON.", e);
        }
    }

    public static void throwFreeplayIfError(
            HttpResponse<String> response,
            int expectedStatus
    ) throws FreeplayException {
        throwIfError(response, expectedStatus, FreeplayClientException::new, FreeplayServerException::new);
    }

    public static void throwLLMIfError(
            HttpResponse<String> response,
            int expectedStatus
    ) throws FreeplayException {
        throwIfError(response, expectedStatus, LLMClientException::new, LLMServerException::new);
    }

    public static void throwIfError(
            HttpResponse<String> response,
            int expectedStatus,
            Function<String, ? extends FreeplayException> clientExceptionCreator,
            Function<String, ? extends FreeplayException> serverExceptionCreator
    ) throws FreeplayException {
        if (response.statusCode() != expectedStatus) {
            String message = format("Error making call [%s]", response.statusCode());
            try {
                Map<String, Object> bodyMap = Http.parseBody(response);
                if (bodyMap.get("message") != null) {
                    message += bodyMap.get("message").toString();
                }
            } catch (Exception ignore) {
            }

            if (response.statusCode() >= 500) {
                throw serverExceptionCreator.apply(message);
            } else {
                throw clientExceptionCreator.apply(message);
            }
        }
    }
}
