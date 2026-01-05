# Freeplay Java SDK

The official Java SDK for easily accessing the Freeplay API.

## Installation

### Apache Maven
```
<dependency>
    <groupId>ai.freeplay</groupId>
    <artifactId>client</artifactId>
    <version>x.x.x</version>
</dependency>
```

### Gradle
```
implementation group: 'ai.freeplay', name: 'client', version: 'x.x.x'
```

## Compatibility

- Java Language Version 11+

## Usage

```java
// Import the SDK
import ai.freeplay.client.Freeplay;

// Initialize the client
Freeplay fpClient = new Freeplay(
        freeplayApiKey,
        baseUrl,
        new OpenAIProviderConfig(openaiApiKey),
        Collections.emptyMap(),
        new HttpConfig(Duration.ofMillis(10_000))
        );

// Completion Request
Map<String, Object> llmParameters = Collections.emptyMap();
CompletionSession session = fpClient.createSession(projectId, "latest");
CompletionResponse response = session.getCompletion(
    "template_name",
    Map.of("input_variable_name", input_variable_value),
    llmParameters
);
```

## Updating Metadata

You can update session and trace metadata at any point after creation. This is useful when you need to associate IDs or information that's generated after a conversation ends:

```java
// Update session metadata
client.metadata().updateSession(
    projectId,
    sessionId,
    Map.of(
        "customer_id", "cust_123",
        "conversation_rating", 5,
        "support_tier", "premium"
    )
).get();  // Wait for completion

// Update trace metadata
client.metadata().updateTrace(
    projectId,
    sessionId,
    traceId,
    Map.of(
        "agent_name", "customer_support_bot",
        "resolution_time_ms", 1234,
        "resolved", true
    )
).get();  // Wait for completion
```

**Merge Semantics**: When you update metadata, new keys are added and existing keys are overwritten. Keys you don't mention are preserved.

See `examples/UpdateMetadataExample.java` for a complete working example.

## Tool Schemas

The SDK supports tool schemas for function calling with LLMs. Tool schemas are passed as `Map` objects in the provider's native format.

### GenAI/Vertex AI Format

Google GenAI/Vertex AI uses a unique format where a single tool contains multiple function declarations:

```java
// Tool schema as raw Map objects (GenAI/Vertex format)
List<Map<String, Object>> toolSchema = List.of(
    Map.of(
        "functionDeclarations", List.of(
            Map.of(
                "name", "get_weather",
                "description", "Get the current weather for a location",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "location", Map.of(
                            "type", "string",
                            "description", "City name, e.g., 'San Francisco'"
                        ),
                        "units", Map.of(
                            "type", "string",
                            "enum", List.of("celsius", "fahrenheit"),
                            "description", "Temperature units"
                        )
                    ),
                    "required", List.of("location")
                )
            ),
            // Multiple functions in a single tool (GenAI-specific)
            Map.of(
                "name", "get_news",
                "description", "Get the latest news articles",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "topic", Map.of("type", "string", "description", "News topic")
                    ),
                    "required", List.of("topic")
                )
            )
        )
    )
);

// Use in recording
long startTime = System.currentTimeMillis();
long endTime = System.currentTimeMillis() + 100;
RecordInfo info = new RecordInfo(projectId, messages)
    .toolSchema(toolSchema)
    .callInfo(new CallInfo("vertex", "gemini-2.0-flash", startTime, endTime, modelParams));

client.recordings().create(info).get();
```

### OpenAI Format

```java
List<Map<String, Object>> toolSchema = List.of(
    Map.of(
        "type", "function",
        "function", Map.of(
            "name", "get_weather",
            "description", "Get weather information",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of("location", Map.of("type", "string")),
                "required", List.of("location")
            )
        )
    )
);

RecordInfo info = new RecordInfo(projectId, messages)
    .toolSchema(toolSchema)
    .callInfo(new CallInfo("openai", "gpt-4", startTime, endTime, modelParams));
```

**Note**: All formats are backward compatible. The backend automatically normalizes tool schemas regardless of format. Tool schemas should be passed as-is from the provider SDK, similar to how messages are handled.
```

## Interactive REPL

For development and testing, use the interactive REPL:

```bash
cd freeplay-jvm

# Production mode (default) - connects to app.freeplay.ai
./scripts/repl.sh

# Local development mode - connects to localhost:8000 with SSL bypass
./scripts/repl.sh --local
```

The REPL provides:
- Pre-loaded imports (Freeplay, RecordInfo, ChatMessage, etc.)
- Environment variables loaded from `.env` file
- Pre-initialized variables (apiKey, baseUrl, projectId, sessionId)
- SSL verification enabled by default (disabled only with --local flag)

Example REPL usage:

```java
// Create client (variables already loaded!)
Freeplay client = new Freeplay(Freeplay.Config().freeplayAPIKey(apiKey).baseUrl(baseUrl));

// Create tool schema as raw Map
List<Map<String, Object>> toolSchema = List.of(
    Map.of(
        "functionDeclarations", List.of(
            Map.of(
                "name", "test",
                "description", "Test function",
                "parameters", Map.of("type", "object")
            )
        )
    )
);

// Record with tool schema
RecordInfo info = new RecordInfo(projectId, List.of(
    new ChatMessage("user", "Hello"),
    new ChatMessage("assistant", "Hi there!")
)).toolSchema(toolSchema)
  .callInfo(new CallInfo("vertex", "gemini-2.0-flash", 0L, 0L, Map.of()));

RecordResponse response = client.recordings().create(info).get();
System.out.println("Completion ID: " + response.getCompletionId());
```

See [JAVA_GENAI_TESTING_GUIDE.md](JAVA_GENAI_TESTING_GUIDE.md) for comprehensive testing instructions with 4 manual test scenarios.

See the [Freeplay Docs](https://docs.freeplay.ai) for more usage examples and the API reference.


## License

This SDK is released under the [MIT License](LICENSE).
