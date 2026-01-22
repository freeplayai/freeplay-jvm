package ai.freeplay.client;

import ai.freeplay.client.internal.CallInfo;
import ai.freeplay.client.internal.PromptInfo;

import java.util.Optional;

public interface RecordProcessor {
    RecordProcessor DO_NOT_RECORD_PROCESSOR = new NoOpRecorder();

    Optional<String> record(PromptInfo promptInfo, CallInfo callInfo);

    class NoOpRecorder implements RecordProcessor {
        @Override
        public Optional<String> record(PromptInfo promptInfo, CallInfo callInfo) {
            // Drop on the floor
            return Optional.empty();
        }
    }
}
