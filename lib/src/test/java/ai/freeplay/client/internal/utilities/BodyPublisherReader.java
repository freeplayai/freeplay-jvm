package ai.freeplay.client.internal.utilities;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

/**
 * Simplifies getting a string from the java.net.http.HttpClient's BodyPublisher interface. This is
 * an involved interface to accommodate higher end use cases. We're in tests and just need the string.
 */
public class BodyPublisherReader implements Flow.Subscriber<ByteBuffer> {

    public static String stringFromBodyPublisher(HttpRequest.BodyPublisher publisher) {
        BodyPublisherReader reader = new BodyPublisherReader();
        publisher.subscribe(reader);
        return reader.getBodyString();
    }

    private final StringBuilder builder = new StringBuilder();

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ByteBuffer item) {
        builder.append(StandardCharsets.UTF_8.decode(item));
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Error in body: " + throwable);
    }

    @Override
    public void onComplete() {
    }

    public String getBodyString() {
        return builder.toString();
    }
}
