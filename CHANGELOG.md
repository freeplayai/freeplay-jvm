# Changelog

Notable additions, fixes, or breaking changes to the Freeplay SDK.

## [0.4.6] - 2026-02-10

### Added

- **`gemini_api_chat` flavor**: New `GeminiApiLLMAdapter` for the Gemini API (`google-genai` SDK). Returns plain `Map` objects for both messages and tool schemas, while `gemini_chat` (`GeminiLLMAdapter`) continues to return Vertex AI protobuf `Content`/`Tool` objects.
- **Gemini message parts passthrough**: Both `GeminiLLMAdapter` and `GeminiApiLLMAdapter` now handle `isGemini()` history messages (function calls, function responses, multi-part content) by reconstructing them in the correct format without re-wrapping.

## [0.4.5] - 2025-12-29

### Added

- Interactive REPL for development and testing:
  - `./scripts/repl.sh` - Production mode (connects to app.freeplay.ai with SSL verification enabled)
  - `./scripts/repl.sh --local` - Local development mode (connects to localhost:8000 with SSL verification disabled)
  - Pre-loaded imports (Freeplay, RecordInfo, ChatMessage, etc.)
  - Environment variables automatically loaded from `.env` file
  - Pre-initialized variables (apiKey, baseUrl, projectId, sessionId)

### Changed

- **Tool Schema Handling**: The SDK no longer provides `GenaiFunction` and `GenaiTool` wrapper classes. Tool schemas should be passed directly as `Map` objects in the provider's native format (e.g., from Google Cloud Vertex AI Java SDK). This aligns with how messages are handled - users pass provider-native types directly to Freeplay.

  ```java
  // Tool schemas are now passed as raw Map objects
  // matching the provider's format
  List<Map<String, Object>> toolSchema = List.of(
      Map.of(
          "functionDeclarations", List.of(
              Map.of(
                  "name", "get_weather",
                  "description", "Get the current weather for a location",
                  "parameters", Map.of(
                      "type", "object",
                      "properties", Map.of(
                          "location", Map.of("type", "string", "description", "City name")
                      ),
                      "required", List.of("location")
                  )
              )
          )
      )
  );

  // Use in recording
  RecordInfo info = new RecordInfo(projectId, messages)
      .toolSchema(toolSchema)
      .callInfo(new CallInfo("vertex", "gemini-2.0-flash", startTime, endTime, modelParams));
  ```
  
  **Notes:**
  - Backend automatically normalizes all tool schema formats (OpenAI, Anthropic, GenAI/Vertex)
  - No breaking changes to the API - tool schemas are still passed the same way
  - This approach is consistent with how we handle messages from different providers

### Added

- Interactive REPL script (`scripts/repl.sh`) for development and testing with:
  - Pre-loaded imports (Freeplay client, etc.)
  - Environment variables automatically loaded from `.env` file
  - SSL verification disabled for local development
  - Pre-initialized variables (apiKey, baseUrl, projectId, sessionId)
  
  ```bash
  ./scripts/repl.sh
  ```

- Comprehensive testing guide (`JAVA_GENAI_TESTING_GUIDE.md`) with manual test scenarios for GenAI tool schema support

- Gradle task `printClasspath` for REPL classpath generation

- New `Metadata` resource for updating session and trace metadata after creation:

  ```java
  // Update session metadata
  client.metadata().updateSession(
      projectId,
      sessionId,
      Map.of(
          "customer_id", "cust_123",
          "rating", 5,
          "status", "resolved"
      )
  ).get();
  
  // Update trace metadata
  client.metadata().updateTrace(
      projectId,
      sessionId,
      traceId,
      Map.of(
          "resolved", true,
          "resolution_time_ms", 1234
      )
  ).get();
  ```

  This addresses the use case where IDs or metadata are generated at the end of a conversation and need to be associated with existing sessions/traces without logging additional completions. Metadata updates use merge semantics - new keys overwrite existing keys while preserving unmentioned keys.

## [0.4.4] - 2025-10-08

### Added

- Add bedrock converse w/ multimedia and tool call support. Add examples.

## [0.4.3] - 2025-10-06

### Added

- New `parentId` parameter in `RecordInfo` to replace the deprecated `traceInfo` parameter. This UUID field enables direct parent-child trace/completions relationships:
  ```java
  // Before (deprecated):
  RecordInfo recordInfo = new RecordInfo(projectId, allMessages)
      .sessionInfo(sessionInfo)
      .traceInfo(traceInfo);
  
  // After:
  RecordInfo recordInfo = new RecordInfo(projectId, allMessages)
      .sessionInfo(sessionInfo)
      .parentId(parentId);  // UUID of parent trace or completion
  ```
- `parentId` parameter support in `Session.createTrace()` methods and `TraceInfo` builder pattern:
  ```java
  TraceInfo parentTrace = session.createTrace("Parent question", "parent_agent", null);  
  TraceInfo trace = session.createTrace("Question", "agent", null)
      .parentId(parentId);  // Chain parentId using builder pattern
  ```

### Changes
- `RecordInfo.traceInfo()` method is deprecated and will be removed in v0.6.0. Use `parentId()` instead for trace hierarchy management.
- `RecordInfo.getTraceInfo()` method is deprecated and will be removed in v0.6.0. Use `getParentId()` instead.
- `Session.createTrace(String input, String agentName, Map<String, Object> customMetadata)` method is deprecated and will be removed in v0.6.0. Use `createTrace(String)` instead and use the builder pattern to set fields on TraceInfo.

## [0.4.2] - 2025-09-10

### Fixed

- Fixed a bug where the `promptVersionInfo` field was not being set correctly in the request body.

## [0.4.1] - 2025-08-28

### Breaking changes

- `RecordInfo` now requires `projectId` as the first parameter. All code creating `RecordInfo` instances must be updated
  to include this field.
- `RecordInfo.fromGeminiContent` method now requires `projectId` as first argument.
- `PromptInfo` no longer contains a `projectId` field. The project ID must now be accessed from the project context
  instead.
- The original `RecordInfo` constructor is removed. Use the new constructor with projectId or the factory methods.
- `RecordInfo.promptInfo()` method has been renamed to `RecordInfo.promptVersionInfo()` and now accepts `PromptVersionInfo` objects. Existing `PromptInfo` objects can still be passed, but the method name must be updated:
  ```java
  // Before:
  new RecordInfo(projectId, allMessages)
      .inputs(variables)
      .promptInfo(formattedPrompt.getPromptInfo())
      .callInfo(callInfo)
  
  // After:
  new RecordInfo(projectId, allMessages)
      .inputs(variables)
      .promptVersionInfo(formattedPrompt.getPromptInfo())
      .callInfo(callInfo)
  ```

### Added

  Use `TestRun.getTraceTestCases()` to retrieve test cases for agent targeting datasets.
- `TraceInfo.recordOutput()` accepts `TestRunInfo` for trace test cases to attach a trace to a test run.
- Support for custom metadata in test cases through `customMetadata` field.

### Changed

- In `RecordInfo`, the following fields are now optional:
    - `inputs` (Optional) - use `.inputs(Map<String, Object>)` to set
    - `promptVersionInfo` (Optional, renamed from `promptInfo`) - use `.promptVersionInfo(PromptVersionInfo)` to set
    - `callInfo` (Optional) - use `.callInfo(CallInfo)` to set
    - `responseInfo` (Optional) - use `.responseInfo(ResponseInfo)` to set
- `SessionInfo` now has a default value and will be automatically generated if not provided
  using `RecordInfo.create(projectId, messages)`.
- New builder-style methods added to `RecordInfo` for setting optional fields.
- Add new optional field `targetEvaluationIds` to `TestRun` creation to control which evaluations run as part of a test.
- `TestCase` class is deprecated. Use `CompletionTestCase` instead for completion-based test runs.

## [0.3.1] - 2025-07-10

### Fixed

- Fixed a precision issue in CallInfo that caused leading "0"s in the millisecond position to be dropped from startTime
  and endTime, resulting in incorrectly calculated latencies.

## [0.3.0] - 2025-06-26

### Breaking changes

- `client.customerFeedback().update()` now requires a `projectId` parameter.

## [0.2.62] - 2025-06-10

### Fixed

- Fixed an issue with trace.recordOutput wherein CompletableFuture was not returned, causing implicit fire and forget
  behavior.
    - Updated return type from void to CompletableFuture<TraceRecordResponse>

## [Before 0.2.61]

See https://docs.freeplay.ai/changelog