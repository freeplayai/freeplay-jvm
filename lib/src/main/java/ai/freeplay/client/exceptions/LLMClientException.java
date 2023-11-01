package ai.freeplay.client.exceptions;

public class LLMClientException extends FreeplayException {
    public LLMClientException(String message) {
        super(message);
    }

    @SuppressWarnings("unused")
    public LLMClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
