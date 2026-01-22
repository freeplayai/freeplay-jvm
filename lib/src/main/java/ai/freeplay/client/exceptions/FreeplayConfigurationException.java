package ai.freeplay.client.exceptions;

public class FreeplayConfigurationException extends FreeplayException {
    public FreeplayConfigurationException(String message) {
        super(message);
    }

    @SuppressWarnings("unused")
    public FreeplayConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
