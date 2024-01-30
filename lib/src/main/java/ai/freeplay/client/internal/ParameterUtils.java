package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayClientException;

import java.util.Map;

public class ParameterUtils {
    public static void validateBasicMap(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!(entry.getValue() instanceof String ||
                    entry.getValue() instanceof Number ||
                    entry.getValue() instanceof Boolean)
            ) {
                throw new FreeplayClientException("Invalid value for key '" + entry.getKey() +
                        "': Value must be a string, number, or boolean.");
            }
        }
    }
}
