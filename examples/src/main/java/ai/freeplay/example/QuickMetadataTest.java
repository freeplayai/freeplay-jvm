package ai.freeplay.example;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.metadata.MetadataUpdateResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Quick test to verify metadata update functionality with real session/trace IDs.
 * 
 * <p>Created to provide a simple, runnable example for testing the metadata update feature
 * with actual session and trace IDs from a running Freeplay environment. This is useful
 * for manual verification and integration testing.</p>
 * 
 * <p>This example demonstrates:</p>
 * <ul>
 *   <li>Updating session metadata with various data types</li>
 *   <li>Updating trace metadata</li>
 *   <li>Merge semantics (updating the same session multiple times)</li>
 * </ul>
 * 
 * Usage:
 *   1. Set environment variables or edit the values below
 *   2. Run: ./gradlew :examples:run -PmainClass=ai.freeplay.example.QuickMetadataTest
 */
public class QuickMetadataTest {
    
    public static void main(String[] args) throws Exception {
        // Configure these values
        String apiKey = System.getenv("FREEPLAY_API_KEY");
        String baseUrl = System.getenv("FREEPLAY_API_URL");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        
        // Or set them directly here for testing:
        // String apiKey = "fp_...";
        // String baseUrl = "https://your-org.freeplay.ai/api";
        // String projectId = "your-project-id";
        
        String sessionId = args.length > 0 ? args[0] : System.getenv("SESSION_ID");
        String traceId = args.length > 1 ? args[1] : System.getenv("TRACE_ID");
        
        if (apiKey == null || baseUrl == null || projectId == null) {
            System.err.println("Error: Set FREEPLAY_API_KEY, FREEPLAY_API_URL, and FREEPLAY_PROJECT_ID");
            System.err.println("Or pass them as arguments to this class");
            System.exit(1);
        }
        
        if (sessionId == null) {
            System.err.println("Error: Provide SESSION_ID as first argument or environment variable");
            System.err.println("Usage: java QuickMetadataTest <session_id> [trace_id]");
            System.exit(1);
        }
        
        System.out.println("============================================================");
        System.out.println("Metadata Update Test");
        System.out.println("============================================================");
        System.out.println("Project ID: " + projectId);
        System.out.println("Session ID: " + sessionId);
        if (traceId != null) {
            System.out.println("Trace ID:   " + traceId);
        }
        System.out.println("============================================================\n");
        
        // Initialize client
        Freeplay client = new Freeplay(
            Freeplay.Config()
                .freeplayAPIKey(apiKey)
                .baseUrl(baseUrl)
        );
        
        // Test 1: Update session metadata
        System.out.println("🔄 Testing session metadata update...");
        try {
            Map<String, Object> sessionMetadata = Map.of(
                    "test_key", "test_value",
                    "test_number", 42,
                    "test_boolean", true,
                    "test_float", 3.14,
                    "timestamp", System.currentTimeMillis()
            );
            
            CompletableFuture<MetadataUpdateResponse> sessionUpdate = 
                client.metadata().updateSession(projectId, sessionId, sessionMetadata);
            
            sessionUpdate.get();  // Wait for completion
            System.out.println("✅ Session metadata updated successfully!");
            System.out.println("   Updated with: " + sessionMetadata);
        } catch (Exception e) {
            System.err.println("❌ Session metadata update failed:");
            System.err.println("   " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("   Cause: " + e.getCause().getMessage());
            }
        }
        
        System.out.println();
        
        // Test 2: Update trace metadata (if trace ID provided)
        if (traceId != null) {
            System.out.println("🔄 Testing trace metadata update...");
            try {
                Map<String, Object> traceMetadata = Map.of(
                        "test_trace_key", "test_trace_value",
                        "test_number", 99,
                        "test_resolved", false,
                        "timestamp", System.currentTimeMillis()
                );
                
                CompletableFuture<MetadataUpdateResponse> traceUpdate = 
                    client.metadata().updateTrace(projectId, sessionId, traceId, traceMetadata);
                
                traceUpdate.get();  // Wait for completion
                System.out.println("✅ Trace metadata updated successfully!");
                System.out.println("   Updated with: " + traceMetadata);
            } catch (Exception e) {
                System.err.println("❌ Trace metadata update failed:");
                System.err.println("   " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("   Cause: " + e.getCause().getMessage());
                }
            }
            System.out.println();
        }
        
        // Test 3: Demonstrate merge semantics
        System.out.println("🔄 Testing merge semantics (updating session again)...");
        try {
            Map<String, Object> additionalMetadata = Map.of(
                    "new_key", "new_value",
                    "test_number", 100  // This will overwrite the previous value
            );
            
            CompletableFuture<MetadataUpdateResponse> mergeUpdate = 
                client.metadata().updateSession(projectId, sessionId, additionalMetadata);
            
            mergeUpdate.get();  // Wait for completion
            System.out.println("✅ Merge update successful!");
            System.out.println("   Added: " + additionalMetadata);
            System.out.println("   Note: 'test_number' was updated to 100, other keys preserved");
        } catch (Exception e) {
            System.err.println("❌ Merge update failed:");
            System.err.println("   " + e.getMessage());
        }
        
        System.out.println();
        System.out.println("============================================================");
        System.out.println("✨ Test complete!");
        System.out.println("============================================================");
    }
}

