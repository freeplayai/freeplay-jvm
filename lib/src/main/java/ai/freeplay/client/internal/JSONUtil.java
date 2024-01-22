package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JSONUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public static <T> T parse(String jsonString, Class<T> targetClass) {
        try {
            return objectMapper.readValue(jsonString, targetClass);
        } catch (JsonProcessingException e) {
            throw new FreeplayException("Unable to parse JSON.", e);
        }
    }

    public static <T> List<T> parseListOf(String jsonString, Class<T> targetClass) {
        try {
            CollectionType javaType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, targetClass);
            return objectMapper.readValue(jsonString, javaType);
        } catch (JsonProcessingException e) {
            throw new FreeplayException("Unable to parse JSON.", e);
        }
    }
}
