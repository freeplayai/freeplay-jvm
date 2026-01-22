package ai.freeplay.client.exceptions;

public class LLMServerException extends FreeplayException {
    public LLMServerException(String message) {
        super(message);
    }

    public LLMServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
