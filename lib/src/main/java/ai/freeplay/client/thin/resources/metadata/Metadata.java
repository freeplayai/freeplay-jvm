package ai.freeplay.client.thin.resources.metadata;

import ai.freeplay.client.thin.internal.ThinCallSupport;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Metadata resource for updating session and trace metadata after creation.
 * 
 * <p>Created to address the customer pain point where IDs and metadata need to be associated
 * with sessions/traces after a conversation ends (e.g., ticket IDs, summary IDs, resolution
 * status generated post-conversation). Without this feature, users had to log dummy completions
 * just to update metadata.</p>
 * 
 * <p>This class provides a clean API for metadata updates without additional trace/session
 * creation. It exposes two main methods:</p>
 * <ul>
 *   <li>{@link #updateSession(String, String, Map)} - Updates session metadata</li>
 *   <li>{@link #updateTrace(String, String, String, Map)} - Updates trace metadata</li>
 * </ul>
 * 
 * <p>This resource allows you to associate IDs and metadata with sessions/traces
 * after the conversation ends (e.g., ticket IDs, summary IDs, resolution status
 * generated post-conversation).</p>
 * 
 * <p>Uses merge semantics: new keys overwrite existing keys, preserving
 * unmentioned keys.</p>
 * 
 * @see <a href="https://docs.freeplay.ai">Freeplay Documentation</a>
 */
public class Metadata {

    private final ThinCallSupport callSupport;

    public Metadata(ThinCallSupport callSupport) {
        this.callSupport = callSupport;
    }

    /**
     * Update session metadata. New keys overwrite existing keys.
     * 
     * @param projectId The project ID
     * @param sessionId The session ID
     * @param metadata Dictionary of metadata key-value pairs to update.
     *                 Supports String, Integer, Long, Float, Double, Boolean values.
     * @return CompletableFuture containing MetadataUpdateResponse
     * @throws ai.freeplay.client.exceptions.FreeplayClientException if session not found
     * @throws ai.freeplay.client.exceptions.FreeplayServerException if server error occurs
     */
    public CompletableFuture<MetadataUpdateResponse> updateSession(
            String projectId,
            String sessionId,
            Map<String, Object> metadata
    ) {
        return callSupport.updateSessionMetadata(projectId, sessionId, metadata);
    }

    /**
     * Update trace metadata. New keys overwrite existing keys.
     * 
     * @param projectId The project ID
     * @param sessionId The session ID (for RESTful hierarchy)
     * @param traceId The trace ID
     * @param metadata Dictionary of metadata key-value pairs to update.
     *                 Supports String, Integer, Long, Float, Double, Boolean values.
     * @return CompletableFuture containing MetadataUpdateResponse
     * @throws ai.freeplay.client.exceptions.FreeplayClientException if trace not found
     * @throws ai.freeplay.client.exceptions.FreeplayServerException if server error occurs
     */
    public CompletableFuture<MetadataUpdateResponse> updateTrace(
            String projectId,
            String sessionId,
            String traceId,
            Map<String, Object> metadata
    ) {
        return callSupport.updateTraceMetadata(projectId, sessionId, traceId, metadata);
    }
}


