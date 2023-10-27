package ai.freeplay.client;

import ai.freeplay.client.internal.CallInfo;
import ai.freeplay.client.internal.PromptInfo;

public interface RecordProcessor {
    RecordProcessor DO_NOT_RECORD_PROCESSOR = new NoOpRecorder();

    void record(PromptInfo promptInfo, CallInfo callInfo);

    class NoOpRecorder implements RecordProcessor {
        @Override
        public void record(PromptInfo promptInfo, CallInfo callInfo) {
            // Drop on the floor
        }
    }
}
