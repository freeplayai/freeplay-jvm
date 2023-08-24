package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;

public class JSONUtil {
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
