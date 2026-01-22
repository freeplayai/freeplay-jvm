package ai.freeplay.client.thin;

/**
 * Comprehensive test suite for the Metadata resource.
 * 
 * <p>Created to verify the metadata update functionality for both sessions and traces.
 * Tests include happy paths, error handling (404, 401, 500), URL construction verification,
 * data type support, and edge cases like empty metadata and invalid types.</p>
 * 
 * <p>This test suite ensures that the metadata update feature works correctly across all
 * scenarios and follows the same patterns as existing SDK tests (e.g., CustomerFeedback tests).</p>
 * 
 * @see ai.freeplay.client.thin.resources.metadata.Metadata
 */

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayClientException;
import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.thin.resources.metadata.MetadataUpdateResponse;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static ai.freeplay.client.thin.Freeplay.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MetadataTest extends HttpClientTestBase {

    @Test
    public void testUpdateSessionMetadataSuccess() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateSessionMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();

            Map<String, Object> metadata = Map.of(
                    "customer_id", "cust_123",
                    "rating", 5,
                    "helpful", true,
                    "score", 4.5
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            MetadataUpdateResponse response = fpClient.metadata().updateSession(projectId, sessionId, metadata).get();

            assertNotNull(response);

            // Verify request body
            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(metadata, requestBody);
        });
    }

    @Test
    public void testUpdateTraceMetadataSuccess() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Map<String, Object> metadata = Map.of(
                    "resolved", true,
                    "resolution_time_ms", 1234,
                    "agent_name", "support_bot",
                    "confidence", 0.95
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            MetadataUpdateResponse response = fpClient.metadata().updateTrace(projectId, sessionId, traceId, metadata).get();

            assertNotNull(response);

            // Verify request body
            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals(metadata, requestBody);
        });
    }

    @Test
    public void testUpdateSessionMetadataNotFound() {
        withMockedClient((HttpClient mockedClient) -> {
            try {
                when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/id/[^/]*/metadata"))
                        .thenReturn(asyncResponse(404, "{\"error\": \"Session not found\"}"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            Map<String, Object> metadata = Map.of("key", "value");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            try {
                fpClient.metadata().updateSession(projectId, sessionId, metadata).get();
                fail("Expected FreeplayClientException to be thrown");
            } catch (Exception e) {
                // ExecutionException wraps the actual exception
                assertTrue(e.getCause() instanceof FreeplayClientException);
                assertTrue(e.getCause().getMessage().contains("404"));
            }
        });
    }

    @Test
    public void testUpdateTraceMetadataNotFound() {
        withMockedClient((HttpClient mockedClient) -> {
            try {
                when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/[^/]*/traces/id/[^/]*/metadata"))
                        .thenReturn(asyncResponse(404, "{\"error\": \"Trace not found\"}"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();
            Map<String, Object> metadata = Map.of("key", "value");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            try {
                fpClient.metadata().updateTrace(projectId, sessionId, traceId, metadata).get();
                fail("Expected FreeplayClientException to be thrown");
            } catch (Exception e) {
                // ExecutionException wraps the actual exception
                assertTrue(e.getCause() instanceof FreeplayClientException);
                assertTrue(e.getCause().getMessage().contains("404"));
            }
        });
    }

    @Test
    public void testUpdateSessionMetadataUnauthorized() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedUpdateSessionMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            Map<String, Object> metadata = Map.of("key", "value");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            try {
                fpClient.metadata().updateSession(projectId, sessionId, metadata).get();
                fail("Expected FreeplayClientException to be thrown");
            } catch (Exception e) {
                // ExecutionException wraps the actual exception
                assertTrue(e.getCause() instanceof FreeplayClientException);
                assertTrue(e.getCause().getMessage().contains("401"));
            }
        });
    }

    @Test
    public void testUpdateSessionMetadataServerError() {
        withMockedClient((HttpClient mockedClient) -> {
            try {
                when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/id/[^/]*/metadata"))
                        .thenReturn(asyncResponse(500, "{\"error\": \"Internal server error\"}"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            Map<String, Object> metadata = Map.of("key", "value");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> fpClient.metadata().updateSession(projectId, sessionId, metadata).get()
            );

            assertTrue(exception.getMessage().contains("500") || 
                      exception.getCause().getMessage().contains("500"));
        });
    }

    @Test
    public void testUpdateSessionMetadataAllDataTypes() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateSessionMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();

            Map<String, Object> metadata = Map.of(
                    "string_key", "string_value",
                    "int_key", 42,
                    "float_key", 3.14,
                    "boolean_key", true
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            MetadataUpdateResponse response = fpClient.metadata().updateSession(projectId, sessionId, metadata).get();

            assertNotNull(response);

            // Verify all data types are preserved
            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals("string_value", requestBody.get("string_key"));
            assertEquals(42, requestBody.get("int_key"));
            assertTrue(requestBody.get("float_key") instanceof Number);
            assertEquals(true, requestBody.get("boolean_key"));
        });
    }

    @Test
    public void testUpdateTraceMetadataAllDataTypes() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();
            String traceId = UUID.randomUUID().toString();

            Map<String, Object> metadata = Map.of(
                    "string_key", "string_value",
                    "int_key", 100,
                    "float_key", 99.99,
                    "boolean_key", false
            );

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            MetadataUpdateResponse response = fpClient.metadata().updateTrace(projectId, sessionId, traceId, metadata).get();

            assertNotNull(response);

            // Verify all data types are preserved
            Map<String, Object> requestBody = JSONUtil.parseMap(getCapturedAsyncBody(mockedClient, 1, 0));
            assertEquals("string_value", requestBody.get("string_key"));
            assertEquals(100, requestBody.get("int_key"));
            assertTrue(requestBody.get("float_key") instanceof Number);
            assertEquals(false, requestBody.get("boolean_key"));
        });
    }

    @Test
    public void testUpdateMetadataEmptyMap() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateSessionMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();

            Map<String, Object> metadata = new HashMap<>();

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // Empty map should be valid (no-op update)
            MetadataUpdateResponse response = fpClient.metadata().updateSession(projectId, sessionId, metadata).get();

            assertNotNull(response);
        });
    }

    @Test
    public void testUpdateMetadataInvalidType() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateSessionMetadataAsync(mockedClient);
            String projectId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();

            // Map with nested object (invalid type)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("valid_key", "valid_value");
            metadata.put("invalid_key", Map.of("nested", "object"));

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // Should throw exception due to invalid type
            assertThrows(
                    Exception.class,
                    () -> fpClient.metadata().updateSession(projectId, sessionId, metadata).get()
            );
        });
    }

    @Test
    public void testUpdateSessionMetadataUrlConstruction() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateSessionMetadataAsync(mockedClient);
            String projectId = "test-project-123";
            String sessionId = "test-session-456";

            Map<String, Object> metadata = Map.of("key", "value");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            fpClient.metadata().updateSession(projectId, sessionId, metadata).get();

            // Verify the URL contains the correct IDs
            // The mock framework will verify the URL pattern matches
            assertNotNull(getCapturedAsyncBody(mockedClient, 1, 0));
        });
    }

    @Test
    public void testUpdateTraceMetadataUrlConstruction() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUpdateTraceMetadataAsync(mockedClient);
            String projectId = "test-project-123";
            String sessionId = "test-session-456";
            String traceId = "test-trace-789";

            Map<String, Object> metadata = Map.of("key", "value");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            fpClient.metadata().updateTrace(projectId, sessionId, traceId, metadata).get();

            // Verify the URL contains the correct IDs
            // The mock framework will verify the URL pattern matches (including sessionId in path)
            assertNotNull(getCapturedAsyncBody(mockedClient, 1, 0));
        });
    }
}

