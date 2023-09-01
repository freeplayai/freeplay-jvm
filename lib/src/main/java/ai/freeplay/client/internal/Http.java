package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.ChatMessage;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.IndexedChatMessage;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.StringUtils.isBlank;
import static java.lang.String.format;
import static java.lang.String.valueOf;

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
            BodyHandler<R> responseBodyHandler
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

    public static class ResponseHandlers {
        public static <R> BodyHandler<Stream<R>> streamHandler(Function<Map<String, Object>, R> itemCreator) {
            return new StreamBodyHandler<>(itemCreator);
        }

        @SuppressWarnings("unchecked")
        public static Function<Map<String, Object>, ChatMessage> chatItemCreator() {
            final AtomicReference<String> role = new AtomicReference<>();
            return (Map<String, Object> choice) -> {
                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                if (delta.get("role") != null) {
                    role.set(valueOf(delta.get("role")));
                }
                boolean isComplete = "stop".equals(choice.get("finish_reason"));
                Object content = delta.get("content");
                // This logic is currently hard-coded to OpenAI's behavior
                if (isBlank(content) && choice.get("finish_reason") != null) {
                    return new IndexedChatMessage(role.get(), "", 0, isComplete, true);
                }

                return new IndexedChatMessage(role.get(), valueOf(content), 0, isComplete, false);
            };
        }

        public static Function<Map<String, Object>, CompletionResponse> textItemCreator() {
            return (Map<String, Object> choice) -> {
                Object text = choice.get("text");
                boolean isComplete = "stop".equals(choice.get("finish_reason"));

                // This logic is currently hard-coded to OpenAI's behavior
                if (isBlank(text) && choice.get("finish_reason") != null) {
                    return new CompletionResponse("", isComplete, true);
                }

                return new CompletionResponse(valueOf(text), isComplete, false);
            };
        }

        public static class StreamBodyHandler<R> implements BodyHandler<Stream<R>> {
            private final Function<Map<String, Object>, R> itemCreator;

            public StreamBodyHandler(Function<Map<String, Object>, R> itemCreator) {
                this.itemCreator = itemCreator;
            }

            @Override
            public BodySubscriber<Stream<R>> apply(HttpResponse.ResponseInfo responseInfo) {
                return new ResponseHandlers.StreamBodySubscriber<>(itemCreator);
            }
        }

        static class StreamBodySubscriber<R> implements BodySubscriber<Stream<R>> {
            // Note on how this works: we are composing the built-in lines subscriber that will do the low level stream
            // reading and breaking it up into lines. We then apply a mapper that parses those lines for the server-sent
            // events (SSE) for the message chunk.
            //
            // The getBody() callback method is what sets up the pipeline. All other methods delegate to the "top"
            // of the pipeline, which is the linesSubscriber. Our mapper is then automatically called downstream of that.

            private final BodySubscriber<Stream<String>> linesSubscriber = BodySubscribers.ofLines(StandardCharsets.UTF_8);
            private final Function<Map<String, Object>, R> itemCreator;

            StreamBodySubscriber(Function<Map<String, Object>, R> itemCreator) {
                this.itemCreator = itemCreator;
            }

            @Override
            public CompletionStage<Stream<R>> getBody() {
                return linesSubscriber.getBody()
                        .thenApply((Stream<String> lines) ->
                                lines
                                        .filter(StreamBodySubscriber::emptyLine)
                                        .map(this::parseLine)
                                        .filter(Objects::nonNull));
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                linesSubscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(List<ByteBuffer> item) {
                linesSubscriber.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                linesSubscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                linesSubscriber.onComplete();
            }

            private static boolean emptyLine(String line) {
                return line.trim().length() > 0;
            }

            private R parseLine(String line) {
                String[] field = line.split(":", 2);
                if (field.length == 2 && "data".equals(field[0])) {
                    if ("[DONE]".equals(field[1].trim())) {
                        return null;
                    } else {
                        Map<String, Object> objectMap = JSONUtil.parseMap(field[1]);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) objectMap.get("choices");
                        Map<String, Object> firstChoice = choices.get(0);
                        return itemCreator.apply(firstChoice);
                    }
                } else {
                    throw new FreeplayException("Got unknown line in the stream: " + line);
                }
            }
        }
    }
}
