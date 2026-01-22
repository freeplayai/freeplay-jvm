package ai.freeplay.client.internal.utilities;

import com.fasterxml.jackson.jr.ob.JSON;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static ai.freeplay.client.internal.utilities.BodyPublisherReader.stringFromBodyPublisher;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MockMethods {
    public static HttpUrlArgumentMatcher requestMatches(String method, String urlPattern) {
        return new HttpUrlArgumentMatcher(method, urlPattern);
    }

    public static HttpUrlArgumentMatcher requestMatches(String host, String method, String urlPattern) {
        return new HttpUrlArgumentMatcher(host, method, urlPattern);
    }

    public static HttpResponse<String> request(
            HttpClient mockedClient,
            String method,
            String urlRegex
    ) throws Exception {
        return mockedClient.send(argThat(requestMatches(method, urlRegex)), any());
    }

    public static <B> HttpResponse<B> request(
            HttpClient mockedClient,
            String host,
            String method,
            String urlRegex
    ) throws Exception {
        return mockedClient.send(argThat(requestMatches(host, method, urlRegex)), any());
    }

    public static CompletableFuture<HttpResponse<String>> requestAsync(
            HttpClient mockedClient,
            String method,
            String urlRegex
    ) {
        return mockedClient.sendAsync(argThat(requestMatches(method, urlRegex)), any());
    }

    public static <B> HttpResponse<B> response(int statusCode, B body) {
        return new StubHttpResponse<>(statusCode, body);
    }

    public static <B> CompletableFuture<HttpResponse<B>> asyncResponse(int statusCode, B body) {
        return CompletableFuture.completedFuture(new StubHttpResponse<>(statusCode, body));
    }

    public static Map<String, Object> getCapturedBodyAsMap(
            HttpClient mockedClient,
            int totalCalls,
            int index
    ) throws RuntimeException {
        try {
            return JSON.std.mapFrom(getCapturedBody(mockedClient, totalCalls, index));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCapturedBody(
            HttpClient mockedClient, int totalCalls, int index
    ) throws RuntimeException {
        ArgumentCaptor<HttpRequest> recordRequestArg = ArgumentCaptor.forClass(HttpRequest.class);
        try {
            verify(mockedClient, times(totalCalls)).send(recordRequestArg.capture(), any());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<HttpRequest> requests = recordRequestArg.getAllValues();
        assertEquals(totalCalls, requests.size());

        Optional<HttpRequest.BodyPublisher> recordBodyPublisher = requests.get(index).bodyPublisher();
        assertTrue(recordBodyPublisher.isPresent());

        return stringFromBodyPublisher(recordBodyPublisher.get());
    }

    public static String getCapturedAsyncBody(
            HttpClient mockedClient, int totalCalls, int index
    ) throws RuntimeException {
        ArgumentCaptor<HttpRequest> recordRequestArg = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockedClient, times(totalCalls)).sendAsync(recordRequestArg.capture(), any());

        List<HttpRequest> requests = recordRequestArg.getAllValues();
        assertEquals(totalCalls, requests.size());

        Optional<HttpRequest.BodyPublisher> recordBodyPublisher = requests.get(index).bodyPublisher();
        assertTrue(recordBodyPublisher.isPresent());

        return stringFromBodyPublisher(recordBodyPublisher.get());
    }

    public static boolean routeNotCalled(
            HttpClient mockedClient, int totalCalls, String urlPart
    ) throws RuntimeException {
        ArgumentCaptor<HttpRequest> recordRequestArg = ArgumentCaptor.forClass(HttpRequest.class);
        try {
            verify(mockedClient, times(totalCalls)).send(recordRequestArg.capture(), any());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<HttpRequest> requests = recordRequestArg.getAllValues();
        for (HttpRequest request : requests) {
            if (request.uri().getPath().contains(urlPart)) {
                return false;
            }
        }
        return true;
    }

    public static HttpRequest getCapturedRequest(
            HttpClient mockedClient, int totalCalls, int index
    ) throws RuntimeException {
        ArgumentCaptor<HttpRequest> recordRequestArg = ArgumentCaptor.forClass(HttpRequest.class);
        try {
            verify(mockedClient, times(totalCalls)).send(recordRequestArg.capture(), any());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<HttpRequest> requests = recordRequestArg.getAllValues();
        assertEquals(totalCalls, requests.size());

        return requests.get(index);
    }
}
