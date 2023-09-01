package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.Map;

public class JSONUtil {
    public static Map<String, Object> parseMap(String json) {
        try {
            return JSON.std
                    .mapFrom(json);
        } catch (IOException e) {
            throw new FreeplayException("Error parsing JSON.", e);
        }
    }

    public static String asString(Object object) {
        try {
            return JSON.std
                    .with(JSON.Feature.WRITE_NULL_PROPERTIES)
                    .asString(object);
        } catch (IOException e) {
            throw new FreeplayException("Error serializing JSON.", e);
        }
    }
}
