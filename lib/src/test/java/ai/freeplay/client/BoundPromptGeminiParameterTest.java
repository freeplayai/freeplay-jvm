package ai.freeplay.client;

import ai.freeplay.client.resources.prompts.BoundPrompt;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FormattedPrompt;
import ai.freeplay.client.resources.prompts.PromptInfo;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class BoundPromptGeminiParameterTest {

    private PromptInfo geminiApiPromptInfo(Map<String, Object> modelParams) {
        return new PromptInfo(
                "test-id",
                "test-version-id",
                "test-template",
                "production",
                modelParams,
                "gemini",
                "gemini-2.5-flash",
                "gemini_api_chat"
        );
    }

    private PromptInfo openaiPromptInfo(Map<String, Object> modelParams) {
        return new PromptInfo(
                "test-id",
                "test-version-id",
                "test-template",
                "production",
                modelParams,
                "openai",
                "gpt-4",
                "openai_chat"
        );
    }

    private List<ChatMessage> simpleMessages() {
        return List.of(new ChatMessage("user", "Hello"));
    }

    @Test
    public void testGeminiApiChatMapsMaxTokens() {
        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", 1024);
        params.put("temperature", 0.7);

        BoundPrompt bound = new BoundPrompt(geminiApiPromptInfo(params), simpleMessages());
        FormattedPrompt<?> formatted = bound.format();

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();
        assertFalse(resultParams.containsKey("max_tokens"));
        assertEquals(1024, resultParams.get("max_output_tokens"));
        assertEquals(0.7, resultParams.get("temperature"));
    }

    @Test
    public void testGeminiApiChatMapsThinkingLevelString() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", "high");

        BoundPrompt bound = new BoundPrompt(geminiApiPromptInfo(params), simpleMessages());
        FormattedPrompt<?> formatted = bound.format();

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();
        assertFalse(resultParams.containsKey("thinking_level"));

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) resultParams.get("thinking_config");
        assertEquals("high", thinkingConfig.get("thinking_level"));
    }

    @Test
    public void testGeminiApiChatMapsThinkingLevelNumeric() {
        Map<String, Object> params = new HashMap<>();
        params.put("thinking_level", 2048);

        BoundPrompt bound = new BoundPrompt(geminiApiPromptInfo(params), simpleMessages());
        FormattedPrompt<?> formatted = bound.format();

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) resultParams.get("thinking_config");
        assertEquals(2048, thinkingConfig.get("thinking_budget"));
    }

    @Test
    public void testGeminiFlavorOverrideMapsParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", 512);

        PromptInfo openai = openaiPromptInfo(params);
        BoundPrompt bound = new BoundPrompt(openai, simpleMessages());

        FormattedPrompt<?> formatted = bound.format("gemini_api_chat");

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();
        assertFalse(resultParams.containsKey("max_tokens"));
        assertEquals(512, resultParams.get("max_output_tokens"));
        assertEquals("gemini_api_chat", formatted.getPromptInfo().getFlavorName());
    }

    @Test
    public void testNonGeminiFlavorDoesNotMapParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", 1024);
        params.put("temperature", 0.7);

        PromptInfo openai = openaiPromptInfo(params);
        BoundPrompt bound = new BoundPrompt(openai, simpleMessages());
        FormattedPrompt<?> formatted = bound.format();

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();
        assertEquals(1024, resultParams.get("max_tokens"));
        assertFalse(resultParams.containsKey("max_output_tokens"));
    }

    @Test
    public void testGeminiApiChatCombinedMapping() {
        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", 4096);
        params.put("temperature", 0.9);
        params.put("thinking_level", "low");
        params.put("top_p", 0.95);

        BoundPrompt bound = new BoundPrompt(geminiApiPromptInfo(params), simpleMessages());
        FormattedPrompt<?> formatted = bound.format();

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();
        assertEquals(4096, resultParams.get("max_output_tokens"));
        assertEquals(0.9, resultParams.get("temperature"));
        assertEquals(0.95, resultParams.get("top_p"));
        assertFalse(resultParams.containsKey("max_tokens"));
        assertFalse(resultParams.containsKey("thinking_level"));

        @SuppressWarnings("unchecked")
        Map<String, Object> thinkingConfig = (Map<String, Object>) resultParams.get("thinking_config");
        assertEquals("low", thinkingConfig.get("thinking_level"));
    }

    @Test
    public void testGeminiWithNoMappableParamsPreservesAllParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.5);
        params.put("top_k", 40);

        BoundPrompt bound = new BoundPrompt(geminiApiPromptInfo(params), simpleMessages());
        FormattedPrompt<?> formatted = bound.format();

        Map<String, Object> resultParams = formatted.getPromptInfo().getModelParameters();
        assertEquals(0.5, resultParams.get("temperature"));
        assertEquals(40, resultParams.get("top_k"));
    }
}
