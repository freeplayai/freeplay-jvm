package ai.freeplay.client.internal.utilities;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.*;

public class MockPayloads {

    public static String getSessionRequestPayload(String sessionId) {
        try {
            return JSON.std.asString(
                    object(
                            "session_id", sessionId
                    ));
        } catch (IOException e) {
            throw new RuntimeException("Invalid MockPayload setup", e);
        }
    }

    public static String getPromptsPayload(
            String projectVersionId,
            String promptTemplateId,
            String promptTemplateVersionId,
            String name,
            String content) {
        try {
            return JSON.std.asString(object(
                    "templates", array(
                            object(
                                    "project_version_id", projectVersionId,
                                    "prompt_template_id", promptTemplateId,
                                    "prompt_template_version_id", promptTemplateVersionId,
                                    "flavor_name", "openai_text",
                                    "name", name,
                                    "content", content
                            )
                    )
            ));
        } catch (IOException e) {
            throw new RuntimeException("Invalid MockPayload setup", e);
        }
    }

    public static String getPromptsPayload(
            String projectVersionId,
            String promptTemplateId,
            String promptTemplateVersionId,
            String name,
            String content,
            Map<String, Object> llmParameters
    ) {
        try {
            return JSON.std.asString(object(
                    "templates", array(
                            object(
                                    "project_version_id", projectVersionId,
                                    "prompt_template_id", promptTemplateId,
                                    "prompt_template_version_id", promptTemplateVersionId,
                                    "flavor_name", "openai_text",
                                    "name", name,
                                    "content", content,
                                    "params", llmParameters
                            )
                    )
            ));
        } catch (IOException e) {
            throw new RuntimeException("Invalid MockPayload setup", e);
        }
    }

    public static String getOpenAITextResponse(String model, String response) {
        return "{\n" +
                "  \"warning\": \"This model version is deprecated. Migrate before January 4, 2024 to avoid disruption of service. Learn more https://platform.openai.com/docs/deprecations\",\n" +
                "  \"id\": \"cmpl-7pjGh1KgUrY1eDYfTsZqK9pFfAzmj\",\n" +
                "  \"object\": \"text_completion\",\n" +
                "  \"created\": 1692563095,\n" +
                "  \"model\": \"" + model + "\",\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"text\": \"" + response + "\",\n" +
                "      \"index\": 0,\n" +
                "      \"logprobs\": null,\n" +
                "      \"finish_reason\": \"length\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"usage\": {\n" +
                "    \"prompt_tokens\": 11,\n" +
                "    \"completion_tokens\": 16,\n" +
                "    \"total_tokens\": 27\n" +
                "  }\n" +
                "}\n";
    }

    public static String getOpenAIUnauthorizedResponse() {
        return "{\n" +
                "  \"error\": {\n" +
                "    \"message\": \"Incorrect API key provided: not-valid. You can find your API key at https://platform.openai.com/account/api-keys.\",\n" +
                "    \"type\": \"invalid_request_error\",\n" +
                "    \"param\": null,\n" +
                "    \"code\": \"invalid_api_key\"\n" +
                "  }\n" +
                "}\n";
    }

    private static Map<String, Object> object(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0)
            throw new IllegalArgumentException("Must have even number of args for keys and values");

        Map<String, Object> object = new HashMap<>(keysAndValues.length);

        for (int i = 0; i < keysAndValues.length; i += 2) {
            object.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return object;
    }

    @SafeVarargs
    private static List<Object> array(Map<String, Object>... objects) {
        return Arrays.asList((Object[]) objects);
    }
}
