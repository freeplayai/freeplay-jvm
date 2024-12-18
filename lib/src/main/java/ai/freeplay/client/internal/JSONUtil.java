package ai.freeplay.client.internal;

import ai.freeplay.client.exceptions.FreeplayException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JSONUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);

    public static Map<String, Object> parseMap(String json) {
        try {
            return JSON.std
                    .mapFrom(json);
        } catch (IOException e) {
            throw new FreeplayException("Error parsing JSON.", e);
        }
    }

    public static List<Object> parseList(String json) {
        try {
            return JSON.std
                    .listFrom(json);
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

    public static JsonNode parseDOM(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new FreeplayException("Error parsing JSON.", e);
        }
    }

    public static String toString(Object thing) {
        try {
            return objectMapper.writeValueAsString(thing);
        } catch (JsonProcessingException e) {
            throw new FreeplayException("Unable to write JSON.", e);
        }
    }

    public static Map<String, Object> nodeToMap(JsonNode paramsNode) {
        return objectMapper.convertValue(paramsNode, new TypeReference<>() {
        });
    }
}
