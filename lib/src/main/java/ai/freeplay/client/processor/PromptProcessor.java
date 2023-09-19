package ai.freeplay.client.processor;

import java.util.function.BiFunction;

public interface PromptProcessor<P> extends BiFunction<P, LLMCallInfo, P> {
}
