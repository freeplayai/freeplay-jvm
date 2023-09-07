package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;

import static java.lang.String.format;

public class Http {

    public static HttpResponse<String> postWithBearer(String url, String apiKey) throws FreeplayException {
        return postJsonWithBearer(url, (String) null, apiKey, BodyHandlers.ofString());
    }

    @SuppressWarnings("UnusedReturnValue")
    public static HttpResponse<String> postJsonWithBearer(
            String url,
            Map<String, Object> body,
            String apiKey
    ) throws FreeplayException {
        String jsonString = JSONUtil.asString(body);
        return postJsonWithBearer(url, jsonString, apiKey, BodyHandlers.ofString());
    }

    public static <R> HttpResponse<R> postJsonWithBearer(
            String url,
            Map<String, Object> body,
            String apiKey,
            BodyHandler<R> responseBodyHandler
    ) throws FreeplayException {
        return postJsonWithBearer(url, JSONUtil.asString(body), apiKey, responseBodyHandler);
    }

    public static <R> HttpResponse<R> postJsonWithBearer(
            String url,
            String body,
            String apiKey,
            BodyHandler<R> responseBodyHandler,
            String... headers
    ) throws FreeplayException {
        List<String> allHeadersArray = new ArrayList<>(headers.length + 3);
        Collections.addAll(allHeadersArray, headers);
        allHeadersArray.add("Authorization");
        allHeadersArray.add(format("Bearer %s", apiKey));
        String[] allHeaders = allHeadersArray.toArray(new String[]{});

        return postJson(url, body, responseBodyHandler, allHeaders);
    }

    public static HttpResponse<String> postJson(
            String url,
            Map<String, Object> body,
            String... headers
    ) throws FreeplayException {
        return postJson(url, body, BodyHandlers.ofString(), headers);
    }

    public static <R> HttpResponse<R> postJson(
            String url,
            Map<String, Object> body,
            BodyHandler<R> responseBodyHandler,
            String... headers
    ) throws FreeplayException {
        String jsonString = JSONUtil.asString(body);
        return postJson(url, jsonString, responseBodyHandler, headers);
    }

    public static <R> HttpResponse<R> postJson(
            String url,
            String body,
            BodyHandler<R> responseBodyHandler,
            String... headers
    ) throws FreeplayException {
        HttpRequest.BodyPublisher bodyPublisher = body != null ?
                HttpRequest.BodyPublishers.ofString(body) :
                HttpRequest.BodyPublishers.noBody();
        HttpRequest.Builder request;
        try {
            request = HttpRequest
                    .newBuilder(new URI(url))
                    .header("Content-Type", "application/json")
                    .headers(headers)
                    .POST(bodyPublisher);
        } catch (URISyntaxException e) {
            throw new FreeplayException("Error in URL during POST request. ", e);
        }

        try {
            return HttpClient
                    .newHttpClient()
                    .send(request.build(), responseBodyHandler);
        } catch (Exception e) {
            throw new FreeplayException("Error sending POST request.", e);
        }
    }

    public static HttpResponse<String> get(String url, String apiKey) throws FreeplayException {
        HttpRequest.Builder request;
        try {
            request = HttpRequest.newBuilder(new URI(url));
        } catch (URISyntaxException e) {
            throw new FreeplayException("Error in URL during GET request. ", e);
        }

        if (apiKey != null) {
            request.header("Authorization", format("Bearer %s", apiKey));
        }

        try {
            return HttpClient
                    .newHttpClient()
                    .send(request.build(), BodyHandlers.ofString());
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

    public static void throwIfError(HttpResponse<String> response, int expectedStatus) throws FreeplayException {
        if (response.statusCode() != expectedStatus) {
            Map<String, Object> bodyMap = Http.parseBody(response);
            Object message = bodyMap.get("message");
            if (message != null) {
                throw new FreeplayException(String.format("Error making call [%s]: %s", response.statusCode(), message));
            } else {
                throw new FreeplayException(String.format("Error making call [%s]", response.statusCode()));
            }
        }
    }

}
