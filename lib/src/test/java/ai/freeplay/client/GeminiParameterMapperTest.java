package ai.freeplay.client;

import ai.freeplay.client.adapters.GeminiParameterMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GeminiParameterMapperTest {

    @Test
    public void testNullParamsReturnsNull() {
        assertNull(GeminiParameterMapper.mapForGemini(null));
    }

    @Test
    public void testEmptyParamsReturnsEmpty() {
        Map<String, Object> empty = new HashMap<>();
        Map<String, Object> result = GeminiParameterMapper.mapForGemini(empty);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMaxTokensRenamedToMaxOutputTokens() {
        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", 1024);
        params.put("temperature", 0.7);

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        assertFalse(result.containsKey("max_tokens"));
        assertEquals(1024, result.get("max_output_tokens"));
        assertEquals(0.7, result.get("temperature"));
    }

    @Test
    public void testTemperaturePassedThrough() {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.5);

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        assertEquals(0.5, result.get("temperature"));
    }

    @Test
    public void testThinkingLevelStringConverted() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", "high");

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        assertFalse(result.containsKey("thinking_level"));
        assertTrue(result.containsKey("thinking_config"));

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) result.get("thinking_config");
        assertEquals("high", thinkingConfig.get("thinking_level"));
        assertFalse(thinkingConfig.containsKey("thinking_budget"));
    }

    @Test
    public void testThinkingLevelStringTrimmedAndLowered() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", "  Medium  ");

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) result.get("thinking_config");
        assertEquals("medium", thinkingConfig.get("thinking_level"));
    }

    @Test
    public void testThinkingLevelNumericIntConverted() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", 2048);

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) result.get("thinking_config");
        assertEquals(2048, thinkingConfig.get("thinking_budget"));
        assertFalse(thinkingConfig.containsKey("thinking_level"));
    }

    @Test
    public void testThinkingLevelNumericDoubleConverted() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", 1024.7);

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) result.get("thinking_config");
        assertEquals(1024, thinkingConfig.get("thinking_budget"));
    }

    @Test
    public void testThinkingLevelUnknownTypeFallsBackToString() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", true);

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) result.get("thinking_config");
        assertEquals("true", thinkingConfig.get("thinking_level"));
    }

    @Test
    public void testAllTransformationsTogether() {
        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", 4096);
        params.put("temperature", 0.9);
        params.put("thinking_level", "low");
        params.put("top_p", 0.95);

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        assertEquals(4096, result.get("max_output_tokens"));
        assertEquals(0.9, result.get("temperature"));
        assertEquals(0.95, result.get("top_p"));

        assertFalse(result.containsKey("max_tokens"));
        assertFalse(result.containsKey("thinking_level"));

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) result.get("thinking_config");
        assertEquals("low", thinkingConfig.get("thinking_level"));
    }

    @Test
    public void testOtherKeysPassedThrough() {
        Map<String, Object> params = new HashMap<>();
        params.put("top_k", 40);
        params.put("stop_sequences", "STOP");

        Map<String, Object> result = GeminiParameterMapper.mapForGemini(params);

        assertEquals(40, result.get("top_k"));
        assertEquals("STOP", result.get("stop_sequences"));
    }
}
