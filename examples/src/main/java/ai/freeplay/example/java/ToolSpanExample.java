package ai.freeplay.example.java;

import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.sessions.CreateTracePayload;
import ai.freeplay.client.thin.resources.sessions.Session;
import ai.freeplay.client.thin.resources.sessions.SpanKind;
import ai.freeplay.client.thin.resources.sessions.TraceInfo;

import java.util.HashMap;
import java.util.Map;

import static ai.freeplay.client.thin.Freeplay.Config;
import static java.lang.String.format;

public class ToolSpanExample {
    public static void main(String[] args) throws Exception {
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME");
        
        Freeplay fpClient = new Freeplay(Config()
            .freeplayAPIKey(freeplayApiKey)
            .customerDomain(customerDomain)
        );
        
        Session session = fpClient.sessions().create();

        // 1. Create a top-level trace for user query (backward compatible string input)
        TraceInfo userTrace = session.createTrace("What is the weather in San Francisco?");
        
        // 2. Create a tool call span as a child of the user trace
        Map<String, Object> toolInput = new HashMap<>();
        toolInput.put("query", "weather San Francisco");
        toolInput.put("location", "San Francisco");
        
        TraceInfo toolTrace = session.createTrace(
            new CreateTracePayload(toolInput)
                .kind(SpanKind.TOOL)
                .name("tavily_search")
                .parentId(userTrace.getTraceId())
        );
        
        // Simulate tool execution
        Map<String, Object> toolOutput = new HashMap<>();
        toolOutput.put("temperature", 72);
        toolOutput.put("conditions", "Sunny");
        toolOutput.put("humidity", "65%");
        
        toolTrace.recordOutput(projectId, toolOutput).get();
        
        // 3. Complete the user trace with final response
        userTrace.recordOutput(projectId, "The weather in San Francisco is sunny and 72°F.").get();
        
        System.out.println("✅ Created hierarchical trace with tool span");
        System.out.println("Session ID: " + session.getSessionId());
        System.out.println("User Trace ID: " + userTrace.getTraceId());
        System.out.println("Tool Trace ID: " + toolTrace.getTraceId());
    }
}

