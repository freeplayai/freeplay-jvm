package ai.freeplay.client;

import org.junit.Before;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.http.HttpClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class HttpClientTestBase {
    private HttpClient mockedClient;
    private HttpClient.Builder mockedClientBuilder;

    @Before
    public void beforeEach() {
        mockedClient = mock(HttpClient.class);
        mockedClientBuilder = mock(HttpClient.Builder.class);
    }

    protected void withMockedClient(ThrowingConsumer<HttpClient> consumer) throws RuntimeException {
        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newBuilder).thenReturn(mockedClientBuilder);
            when(mockedClientBuilder.build()).thenReturn(mockedClient);
            consumer.accept(mockedClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface ThrowingConsumer<Value> {
        void accept(Value value) throws Exception;
    }
}
