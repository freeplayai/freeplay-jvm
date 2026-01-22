package ai.freeplay.client.exceptions;

public class FreeplayClientException extends FreeplayException {
    public FreeplayClientException(String message) {
        super(message);
    }

    public FreeplayClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
