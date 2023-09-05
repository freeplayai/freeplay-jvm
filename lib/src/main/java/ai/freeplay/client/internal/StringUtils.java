package ai.freeplay.client.internal;

public class StringUtils {
    public static boolean isBlank(Object value) {
        return isBlank((String) value);
    }

    public static boolean isBlank(String value) {
        return value == null ||
                value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
