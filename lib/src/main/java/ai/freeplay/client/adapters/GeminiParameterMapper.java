package ai.freeplay.client.adapters;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Freeplay model parameters to the Gemini / Vertex AI generation config format.
 *
 * <ul>
 *   <li>{@code max_tokens} is renamed to {@code max_output_tokens}</li>
 *   <li>{@code thinking_level} is converted to a {@code thinking_config} map with
 *       either {@code thinking_level} (string values) or {@code thinking_budget}
 *       (numeric values)</li>
 *   <li>All other keys (including {@code temperature}) are passed through unchanged.</li>
 * </ul>
 */
public final class GeminiParameterMapper {

    private GeminiParameterMapper() {}

    public static Map<String, Object> mapForGemini(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return params;
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("max_tokens".equals(key)) {
                result.put("max_output_tokens", value);
            } else if ("thinking_level".equals(key)) {
                result.put("thinking_config", thinkingLevelToConfig(value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    static Map<String, Object> thinkingLevelToConfig(Object level) {
        Map<String, Object> config = new HashMap<>();
        if (level instanceof String) {
            config.put("thinking_level", ((String) level).trim().toLowerCase());
        } else if (level instanceof Number) {
            config.put("thinking_budget", ((Number) level).intValue());
        } else {
            config.put("thinking_level", String.valueOf(level));
        }
        return config;
    }
}
