package ai.freeplay.client.internal.utilities;

import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.PromptTemplate;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.utilities.MockMethods.request;
import static ai.freeplay.client.internal.utilities.MockMethods.response;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.when;

public class MockFixtures {

    public static final String baseUrl = "http://localhost:8080/api";

    public static final String MODEL_GPT_35_TURBO = "gpt-3.5-turbo";
    public static final String MODEL_CLAUDE_2 = "claude-2";

    public static final String freeplayApiKey = "<freeplay-api-key>";
    public static final String openaiApiKey = "<openai-api-key>";
    public static final String anthropicApiKey = "<anthropic-api-key>";
    public static final String projectId = UUID.randomUUID().toString();

    public static final String projectVersionId = UUID.randomUUID().toString();
    public static final String promptTemplateId = UUID.randomUUID().toString();
    public static final String promptTemplateVersionId = UUID.randomUUID().toString();

    // Freeplay
    public static void mockRecord(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "POST", "v1/record"))
                    .thenReturn(
                            response(201,
                                    JSONUtil.asString(
                                            object(
                                                    "completion_id", UUID.randomUUID().toString()
                                            ))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockGetPrompts(
            HttpClient mockedClient,
            String model,
            String templateName,
            String templateContent
    ) throws RuntimeException {
        Map<String, Object> llmParameters = new HashMap<>();
        if (model != null)
            llmParameters.put("model", model);

        try {
            mockGetPrompts(mockedClient, templateName, templateContent, llmParameters, "openai_chat");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockGetPrompts(
            HttpClient mockedClient,
            String templateName,
            String content,
            Map<String, Object> llmParameters,
            String flavor
    ) throws RuntimeException {
        try {
            when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                    .thenReturn(
                            response(
                                    200,
                                    getPromptsPayload(
                                            flavor,
                                            projectVersionId,
                                            promptTemplateId,
                                            promptTemplateVersionId,
                                            templateName,
                                            content,
                                            llmParameters)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockGet2Prompts(
            HttpClient mockedClient,
            PromptTemplate... templates
    ) throws RuntimeException {

        List<Map<String, Object>> templateObjects = stream(templates).map((PromptTemplate template) ->
                object(
                        "project_version_id", template.getProjectVersionId(),
                        "prompt_template_id", template.getPromptTemplateId(),
                        "prompt_template_version_id", template.getPromptTemplateVersionId(),
                        "flavor_name", template.getFlavorName(),
                        "name", template.getName(),
                        "content", template.getContent(),
                        "params", template.getLLMParameters()
                )).collect(toList());

        Map<String, Object> payload = object(
                "templates", array(
                        (Object[]) templateObjects.toArray(new Map[]{})
                )
        );

        try {
            when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                    .thenReturn(response(200, JSONUtil.asString(payload)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockCreateTestRun(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "POST", "projects/[^/]*/test-runs"))
                    .thenReturn(
                            response(201, getTestRunResponsePayload(UUID.randomUUID().toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedCreateSession(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "POST", "projects/[^/]*/sessions"))
                    .thenReturn(response(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedGetPrompts(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                    .thenReturn(response(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockOpenAIChatCalls(HttpClient mockedClient, String... completions) throws RuntimeException {
        @SuppressWarnings("unchecked")
        HttpResponse<Object>[] responses = Arrays.stream(completions)
                .map(completion -> response(200, getOpenAIChatResponse(completion)))
                .toArray(HttpResponse[]::new);

        try {
            when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                    .thenReturn(responses[0], Arrays.copyOfRange(responses, 1, responses.length));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockOpenAIChatCallStream(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                    .thenReturn(
                            response(200, getOpenAIChatResponseStreamMessages()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedOpenAIChatCall(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                    .thenReturn(response(401, getOpenAIUnauthorizedResponse()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Anthropic
    public static void mockAnthropicCall(HttpClient mockedClient, String completion) throws RuntimeException {
        try {
            when(request(mockedClient, "api.anthropic.com", "POST", "v1/complete"))
                    .thenReturn(
                            response(200, getAnthropicResponse(MODEL_CLAUDE_2, completion)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockAnthropicCallStream(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.anthropic.com", "POST", "v1/complete"))
                    .thenReturn(
                            response(200, getAnthropicResponseStreamMessages()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedAnthropicCall(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.anthropic.com", "POST", "v1/complete"))
                    .thenReturn(response(401, getAnthropicUnauthorizedResponse()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String getSessionRequestPayload(String sessionId) {
        return JSONUtil.asString(
                object(
                        "session_id", sessionId
                ));
    }

    public static String getTestRunResponsePayload(String testRunId) {
        return JSONUtil.asString(
                object(
                        "test_run_id", testRunId,
                        "inputs", array(
                                object("question", "Why isn't my sink working?"),
                                object("question", "Why isn't my internet working?")
                        )
                ));
    }

    public static String getPromptsPayload(
            String flavorName,
            String projectVersionId,
            String promptTemplateId,
            String promptTemplateVersionId,
            String templateName,
            String content,
            Map<String, Object> llmParameters
    ) {
        return JSONUtil.asString(object(
                "templates", array(
                        object(
                                "project_version_id", projectVersionId,
                                "prompt_template_id", promptTemplateId,
                                "prompt_template_version_id", promptTemplateVersionId,
                                "flavor_name", flavorName,
                                "name", templateName,
                                "content", content,
                                "params", llmParameters
                        )
                )
        ));
    }

    public static String getChatPromptContent() {
        return JSONUtil.asString(array(
                object(
                        "role", "system",
                        "content", "You are a support agent."
                ),
                object(
                        "role", "assistant",
                        "content", "How may I help you?"
                ),
                object(
                        "role", "user",
                        "content", "{{question}}"
                )
        ));
    }

    public static String getOpenAIChatResponse(String response) {
        return "{\n" +
                "  \"id\": \"chatcmpl-7r34GwVSTw9RCL63j3g3AlCXQ4TSw\",\n" +
                "  \"object\": \"chat.completion\",\n" +
                "  \"created\": 1692877532,\n" +
                "  \"model\": \"gpt-3.5-turbo-0613\",\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"message\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"" + escapeJSON(response) + "\"\n" +
                "      },\n" +
                "      \"finish_reason\": \"stop\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"usage\": {\n" +
                "    \"prompt_tokens\": 52,\n" +
                "    \"completion_tokens\": 12,\n" +
                "    \"total_tokens\": 64\n" +
                "  }\n" +
                "}\n";
    }

    public static Stream<String> getOpenAIChatResponseStreamMessages() {
        return Stream.of(
                "data: {\"id\": \"chatcmpl-7gy0Jd0fz6KhxaHISoAFvRovZVSkJ\", \"object\": \"chat.completion.chunk\", \"created\": 1690474787, \"model\": \"gpt-3.5-turbo-0613\", \"choices\": [{\"index\": 0, \"delta\": {\"role\": \"assistant\", \"content\": \"\"}, \"finish_reason\": null}]}\n\n",
                "data: {\"id\": \"chatcmpl-7gy0Jd0fz6KhxaHISoAFvRovZVSkJ\", \"object\": \"chat.completion.chunk\", \"created\": 1690474787, \"model\": \"gpt-3.5-turbo-0613\", \"choices\": [{\"index\": 0, \"delta\": {\"content\": \"Well \"}, \"finish_reason\": null}] }\n\n",
                "data: {\"id\": \"chatcmpl-7gy0Jd0fz6KhxaHISoAFvRovZVSkJ\", \"object\": \"chat.completion.chunk\", \"created\": 1690474787, \"model\": \"gpt-3.5-turbo-0613\", \"choices\": [{\"index\": 0, \"delta\": {\"content\": \"hello\"}, \"finish_reason\": null}]}\n\n",
                "data: {\"id\": \"chatcmpl-7gy0Jd0fz6KhxaHISoAFvRovZVSkJ\", \"object\": \"chat.completion.chunk\", \"created\": 1690474787, \"model\": \"gpt-3.5-turbo-0613\", \"choices\": [{\"index\": 0, \"delta\": {\"content\": \"\"}, \"finish_reason\": \"length\"}]}"
        );
    }

    public static Stream<String> getOpenAIChatResponseStreamMessages2() {
        return Stream.of(
                "data: {\"id\":\"chatcmpl-7v5w9OEPLPFKBkBw9rJQuN2craSCW\",\"object\":\"chat.completion.chunk\",\"created\":1693841873,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"\"},\"finish_reason\":null}]}\n\n",
                "data: {\"id\":\"chatcmpl-7v5w9OEPLPFKBkBw9rJQuN2craSCW\",\"object\":\"chat.completion.chunk\",\"created\":1693841873,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Bene \"},\"finish_reason\":null}]}\n\n",
                "data: {\"id\":\"chatcmpl-7v5w9OEPLPFKBkBw9rJQuN2craSCW\",\"object\":\"chat.completion.chunk\",\"created\":1693841873,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"ciao\"},\"finish_reason\":null}]}\n\n",
                "data: {\"id\":\"chatcmpl-7v5w9OEPLPFKBkBw9rJQuN2craSCW\",\"object\":\"chat.completion.chunk\",\"created\":1693841873,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n"
        );
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

    public static String getAnthropicResponse(String model, String response) {
        return "{\n" +
                "  \"completion\": \"" + response + "\",\n" +
                "  \"stop_reason\": \"max_tokens\",\n" +
                "  \"model\": \"" + model + "\",\n" +
                "  \"stop\": null,\n" +
                "  \"log_id\": \"3dde119e125c95cea0915fcffa21f47f9c340d76c9ceb6050e34cfe2a277652a\"\n" +
                "}";
    }

    public static Stream<String> getAnthropicResponseStreamMessages() {
        return Stream.of(
                "event: completion",
                "data: {\"completion\":\" Oh\",\"stop_reason\":null,\"model\":\"claude-2.0\",\"stop\":null,\"log_id\":\"d0914d5a1c9eaa87de003830bc290dcf32d4bd6c097b27466b2d65a7c80bf7d7\"}",
                "",
                "event: completion",
                "data: {\"completion\":\" dear\",\"stop_reason\":null,\"model\":\"claude-2.0\",\"stop\":null,\"log_id\":\"d0914d5a1c9eaa87de003830bc290dcf32d4bd6c097b27466b2d65a7c80bf7d7\"}",
                "",
                "event: ping",
                "data: {}",
                "",
                "event: completion",
                "data: {\"completion\":\",\",\"stop_reason\":null,\"model\":\"claude-2.0\",\"stop\":null,\"log_id\":\"d0914d5a1c9eaa87de003830bc290dcf32d4bd6c097b27466b2d65a7c80bf7d7\"}",
                "",
                "event: new-unknown-event",
                "data: ",
                "",
                "event: completion",
                "data: {\"completion\":\" really\",\"stop_reason\":\"max_tokens\",\"model\":\"claude-2.0\",\"stop\":null,\"log_id\":\"d0914d5a1c9eaa87de003830bc290dcf32d4bd6c097b27466b2d65a7c80bf7d7\"}",
                ""
        );
    }

    public static String getAnthropicUnauthorizedResponse() {
        return "{\n" +
                "  \"error\": {\n" +
                "    \"type\": \"authentication_error\",\n" +
                "    \"message\": \"Invalid API Key\"\n" +
                "  }\n" +
                "}";
    }


    public static Map<String, Object> object(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0)
            throw new IllegalArgumentException("Must have even number of args for keys and values");

        Map<String, Object> object = new HashMap<>(keysAndValues.length);

        for (int i = 0; i < keysAndValues.length; i += 2) {
            object.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return object;
    }

    public static List<Object> array(Object... objects) {
        return Arrays.asList(objects);
    }

    public static String escapeJSON(String jsonString) {
        return jsonString.replace("\"", "\\\"");
    }

    public static String unescapeExpected(String completion1) {
        return completion1.replace("\\n", "\n");
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object openAiRequestBody) {
        return (List<Object>) openAiRequestBody;
    }

}
