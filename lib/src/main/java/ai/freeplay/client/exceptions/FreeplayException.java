package ai.freeplay.client.exceptions;

public class FreeplayException extends RuntimeException {
    public FreeplayException(String message) {
        super(message);
    }

    public FreeplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
