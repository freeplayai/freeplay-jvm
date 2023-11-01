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

See the [Freeplay Docs](https://docs.freeplay.ai) for more usage examples and the API reference.


## License

This SDK is released under the [MIT License](LICENSE).
