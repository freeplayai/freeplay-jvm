package ai.freeplay.client.exceptions;

public class FreeplayServerException extends FreeplayException {
    public FreeplayServerException(String message) {
        super(message);
    }

    public FreeplayServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
