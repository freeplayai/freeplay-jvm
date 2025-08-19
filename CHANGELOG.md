# Changelog

Notable additions, fixes, or breaking changes to the Freeplay SDK.

## [0.4.0] - TBD

### Breaking changes

- `RecordInfo` now requires `projectId` as the first parameter. All code creating `RecordInfo` instances must be updated
  to include this field.
- `RecordInfo.fromGeminiContent` method now requires `projectId` as first argument.
- `PromptInfo` no longer contains a `projectId` field. The project ID must now be accessed from the project context
  instead.
- The original `RecordInfo` constructor is removed. Use the new constructor with projectId or the factory methods.

### Added

- Support for trace test cases in test runs through new `TraceTestCase` class for agent datasets.
  Use `TestRun.getTraceTestCases()` to retrieve test cases for agent targeting datasets.
- `TraceInfo.recordOutput()` accepts `TestRunInfo` for trace test cases to attach a trace to a test run.
- Support for custom metadata in test cases through `customMetadata` field.

### Changed

- In `RecordInfo`, the following fields are now optional:
    - `inputs` (Optional) - use `.withInputs(Map<String, Object>)` to set
    - `promptInfo` (Optional) - use `.withPromptInfo(PromptInfo)` to set
    - `callInfo` (Optional) - use `.withCallInfo(CallInfo)` to set
    - `responseInfo` (Optional) - use `.withResponseInfo(ResponseInfo)` to set
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