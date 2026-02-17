<h1 align="center">Freeplay Java SDK</h1>

<p align="center">
  <strong>The official Java/JVM SDK for <a href="https://freeplay.ai">Freeplay</a></strong><br/>
  The ops platform for enterprise AI engineering teams
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/ai.freeplay/client"><img src="https://img.shields.io/maven-central/v/ai.freeplay/client.svg" alt="version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-blue.svg" alt="License" /></a>
</p>

<p align="center">
  <a href="https://docs.freeplay.ai">Docs</a> •
  <a href="https://docs.freeplay.ai/getting-started/overview">Quick Start</a> •
  <a href="https://docs.freeplay.ai/freeplay-sdk/setup">SDK Setup</a> •
  <a href="https://docs.freeplay.ai/developer-resources/api-reference">API Reference</a> •
  <a href="CHANGELOG.md">Changelog</a> •
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

## Overview

Freeplay is the only platform your team needs to manage the end-to-end AI application development lifecycle. It provides an integrated workflow for improving your AI agents and other generative AI products. Engineers, data scientists, product managers, designers, and subject matter experts can all review production logs, curate datasets, experiment with changes, create and run evaluations, and deploy updates.

Use this SDK to integrate with Freeplay's core capabilities:

- **Observability**
  - [**Sessions**](https://docs.freeplay.ai/freeplay-sdk/sessions) — group related interactions together, e.g. for multi-turn chat or complex agent interactions
  - [**Traces**](https://docs.freeplay.ai/freeplay-sdk/traces) — track multi-step agent workflows within sessions
  - [**Completions**](https://docs.freeplay.ai/freeplay-sdk/recording-completions) — record LLM interactions for observability and debugging
  - [**Customer Feedback**](https://docs.freeplay.ai/freeplay-sdk/customer-feedback) — append user feedback and events to traces and completions
- [**Prompts**](https://docs.freeplay.ai/freeplay-sdk/prompts) — version, format, and fetch prompt templates across environments
- [**Test Runs**](https://docs.freeplay.ai/freeplay-sdk/test-runs) — execute evaluation runs against prompts and datasets

## Requirements

- Java 11 or higher
- A Freeplay account + API key

## Installation

### Gradle

```groovy
implementation 'ai.freeplay:client:x.x.x'
```

### Maven

```xml
<dependency>
    <groupId>ai.freeplay</groupId>
    <artifactId>client</artifactId>
    <version>x.x.x</version>
</dependency>
```

### Gemini (Google Vertex AI) Support

The Gemini adapter requires the Google Cloud Vertex AI library, which is an optional dependency not included by default. If your Freeplay prompts target Gemini, you need to add it separately.

**Gradle (feature variant):**

```groovy
implementation('ai.freeplay:client:x.x.x') {
    capabilities {
        requireCapability('ai.freeplay:client-gemini')
    }
}
```

**Gradle (direct dependency):**

```groovy
implementation 'com.google.cloud:google-cloud-vertexai:1.5.0'
```

**Maven:**

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-vertexai</artifactId>
    <version>1.5.0</version>
</dependency>
```
*Replace the version number with the correct current version of the google-cloud-vertexai library.*

All other providers (OpenAI, Anthropic, Bedrock, etc.) work out of the box with no additional dependencies.

## Quick Start

```java
import ai.freeplay.client.thin.Freeplay;
import ai.freeplay.client.thin.resources.prompts.FormattedPrompt;
import ai.freeplay.client.thin.resources.prompts.ChatMessage;
import ai.freeplay.client.thin.resources.recordings.RecordInfo;
import java.util.List;
import java.util.Map;

// Initialize the Freeplay client
Freeplay fpClient = new Freeplay(
    Freeplay.Config()
        .freeplayAPIKey(System.getenv("FREEPLAY_API_KEY"))
);

String projectId = System.getenv("FREEPLAY_PROJECT_ID");

// Fetch a prompt from Freeplay
FormattedPrompt<List<ChatMessage>> formattedPrompt = fpClient.prompts()
    .<List<ChatMessage>>getFormatted(
        projectId,
        "my-prompt",
        "prod",
        Map.of("user_input", "Hello, world!"),
        null
    ).get();

// Call your LLM provider with formattedPrompt.getFormattedPrompt()
// ... your OpenAI/Anthropic/etc. call here ...
String assistantResponse = "Response from LLM";

// Record the interaction for observability
fpClient.recordings().create(
    new RecordInfo(projectId, formattedPrompt.allMessages(
        Map.of("role", "assistant", "content", assistantResponse)
    ))
).get();
```

See the [SDK Setup guide](https://docs.freeplay.ai/freeplay-sdk/setup) for complete examples.

## Configuration

### Environment variables

```bash
export FREEPLAY_API_KEY="fp_..."
export FREEPLAY_PROJECT_ID="xy..."
# Optional: override if using a custom domain / private deployment
export FREEPLAY_API_BASE="https://app.freeplay.ai/api"
```

**API base URL**  
Default: `https://app.freeplay.ai/api`

Custom domain/private deployment: `https://<your-domain>/api`

## Additional Features

### Updating Metadata

Update session and trace metadata at any point after creation:

```java
// Update session metadata
fpClient.metadata().updateSession(
    projectId,
    sessionId,
    Map.of(
        "customer_id", "cust_123",
        "conversation_rating", 5
    )
).get();

// Update trace metadata
fpClient.metadata().updateTrace(
    projectId,
    sessionId,
    traceId,
    Map.of("resolved", true)
).get();
```

**Merge Semantics**: New keys are added, existing keys are overwritten, unmentioned keys are preserved.

### Tool Schemas

The SDK supports tool schemas for function calling. Pass schemas as `Map` objects in the provider's native format:

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
    .callInfo(callInfo);
```

### Interactive REPL

For development and testing:

```bash
# Production mode (default)
./scripts/repl.sh

# Local development mode
./scripts/repl.sh --local
```

The REPL provides pre-loaded imports, environment variables from `.env`, and pre-initialized variables.

## Versioning

This SDK follows Semantic Versioning (SemVer): **MAJOR.MINOR.PATCH**.

- **PATCH**: bug fixes
- **MINOR**: backward-compatible features
- **MAJOR**: breaking changes

Before upgrading major versions, review the changelog.

## Support

- **Docs**: https://docs.freeplay.ai
- **Issues**: https://github.com/freeplayai/freeplay-jvm/issues
- **Security**: security@freeplay.ai

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache-2.0 — see [LICENSE](LICENSE).
