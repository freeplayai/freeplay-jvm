package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static java.lang.String.format;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

public class HttpUtil {

    public static HttpResponse<String> postWithBearer(String url, String apiKey) throws FreeplayException {
        return postJsonWithBearer(url, (String) null, apiKey);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static HttpResponse<String> postJsonWithBearer(
            String url,
            Map<String, Object> body,
            String apiKey
    ) throws FreeplayException {
        String jsonString;
        try {
            jsonString = JSON.std
                    .with(JSON.Feature.WRITE_NULL_PROPERTIES)
                    .asString(body);
        } catch (IOException e) {
            throw new FreeplayException("Error sending POST request.", e);
        }
        return postJsonWithBearer(url, jsonString, apiKey);
    }

    public static HttpResponse<String> postJsonWithBearer(
            String url,
            String body,
            String apiKey
    ) throws FreeplayException {
        HttpRequest.BodyPublisher bodyPublisher = body != null ?
                HttpRequest.BodyPublishers.ofString(body) :
                HttpRequest.BodyPublishers.noBody();
        HttpRequest.Builder request;
        try {
            request = HttpRequest
                    .newBuilder(new URI(url))
                    .header("Content-Type", "application/json")
                    .POST(bodyPublisher);
        } catch (URISyntaxException e) {
            throw new FreeplayException("Error in URL during POST request. ", e);
        }

        if (apiKey != null) {
            request.header("Authorization", format("Bearer %s", apiKey));
        }

        try {
            return HttpClient
                    .newHttpClient()
                    .send(request.build(), ofString());
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
                    .send(request.build(), ofString());
        } catch (Exception e) {
            throw new FreeplayException("Error sending GET request.", e);
        }
    }

    public static Map<String, Object> parseBody(
            HttpResponse<String> response
    ) throws FreeplayException {
        try {
            return JSON.std.mapFrom(response.body());
        } catch (IOException e) {
            throw new FreeplayException("Unable to parse JSON.", e);
        }
    }

    public static void throwIfError(HttpResponse<String> response, int expectedStatus) throws FreeplayException {
        if (response.statusCode() != expectedStatus) {
            Map<String, Object> bodyMap = HttpUtil.parseBody(response);
            Object message = bodyMap.get("message");
            if (message != null) {
                throw new FreeplayException(String.format("Error making call [%s]: %s", response.statusCode(), message));
            } else {
                throw new FreeplayException(String.format("Error making call [%s]", response.statusCode()));
            }
        }
    }
}
