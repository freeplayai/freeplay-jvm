package ai.freeplay.client.internal.utilities;

import org.mockito.ArgumentMatcher;

import java.net.http.HttpRequest;
import java.util.regex.Pattern;

public final class HttpUrlArgumentMatcher implements ArgumentMatcher<HttpRequest> {
    private final String host;
    private final String method;
    private final Pattern urlPattern;

    HttpUrlArgumentMatcher(String method, String urlPattern) {
        this.host = null;
        this.method = method;
        this.urlPattern = Pattern.compile(urlPattern);
    }

    HttpUrlArgumentMatcher(String host, String method, String urlPattern) {
        this.host = host;
        this.method = method;
        this.urlPattern = Pattern.compile(urlPattern);
    }

    @Override
    public boolean matches(HttpRequest argument) {
        if (argument == null) return false;
        if (host != null && !argument.uri().getHost().equals(host)) {
            return false;
        }
        return argument.method().equalsIgnoreCase(method) &&
                urlPattern.matcher(argument.uri().toString()).find();
    }
}
