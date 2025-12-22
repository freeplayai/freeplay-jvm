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

See the [Freeplay Docs](https://docs.freeplay.ai) for more usage examples and the API reference.


## License

This SDK is released under the [MIT License](LICENSE).
