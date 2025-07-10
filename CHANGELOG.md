# Changelog

Notable additions, fixes, or breaking changes to the Freeplay SDK.

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