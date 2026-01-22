package ai.freeplay.client;

import java.net.ProxySelector;
import java.time.Duration;
import java.util.concurrent.Executor;

public class HttpConfig {
    private final Executor executor;
    private final Duration requestTimeout;
    private final ProxySelector proxySelector;

    public HttpConfig(Executor executor) {
        this(executor, null, null);
    }

    public HttpConfig(Duration requestTimeout) {
        this(null, requestTimeout, null);
    }

    public HttpConfig(ProxySelector proxySelector) {
        this(null, null, proxySelector);
    }

    public HttpConfig() {
        this(null, null, null);
    }

    public HttpConfig(Executor executor, Duration requestTimeout, ProxySelector proxySelector) {
        this.executor = executor;
        this.requestTimeout = requestTimeout;
        this.proxySelector = proxySelector;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public ProxySelector getProxySelector() {
        return proxySelector;
    }
}
