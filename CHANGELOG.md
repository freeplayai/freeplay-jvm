# Changelog

Notable additions, fixes, or breaking changes to the Freeplay SDK.

## [0.2.62] - 2025-06-10

### Fixed

- Fixed an issue with trace.recordOutput wherein CompletableFuture was not returned, causing implicit fire and forget
  behavior.
    - Updated return type from void to CompletableFuture<TraceRecordResponse>

## [Before 0.2.61]

See https://docs.freeplay.ai/changelog