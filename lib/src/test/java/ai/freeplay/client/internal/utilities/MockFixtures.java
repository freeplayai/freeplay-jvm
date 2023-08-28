package ai.freeplay.client.internal.utilities;

import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.PromptTemplate;

import java.net.http.HttpClient;
import java.util.*;

import static ai.freeplay.client.internal.utilities.MockMethods.request;
import static ai.freeplay.client.internal.utilities.MockMethods.response;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.when;

public class MockFixtures {

    public static final String baseUrl = "http://localhost:8080/api";

    public static final String MODEL_TEXT_DAVINCI_003 = "text-davinci-003";
    public static final String MODEL_GPT_TURBO_35 = "gpt-turbo-3.5";

    public static final String freeplayApiKey = "<freeplay-api-key>";
    public static final String openaiApiKey = "<openai-api-key>";
    public static final String projectId = UUID.randomUUID().toString();

    public static final String projectVersionId = UUID.randomUUID().toString();
    public static final String promptTemplateId = UUID.randomUUID().toString();
    public static final String promptTemplateVersionId = UUID.randomUUID().toString();

    public static void mockCreateSession(HttpClient mockedClient) throws Exception {
        when(request(mockedClient, "POST", "projects/[^/]*/sessions"))
                .thenReturn(
                        response(201, getSessionRequestPayload(UUID.randomUUID().toString())));
    }

    public static void mockGetPrompts(
            HttpClient mockedClient,
            String model,
            String templateName,
            String templateContent
    ) throws Exception {
        Map<String, Object> llmParameters = new HashMap<>();
        if (model != null)
            llmParameters.put("model", model);

        mockGetPrompts(mockedClient, templateName, templateContent, llmParameters, "openai_chat");
    }

    public static void mockGetPrompts(
            HttpClient mockedClient,
            String templateName,
            String content,
            Map<String, Object> llmParameters,
            String flavor
    ) throws Exception {
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
    }

    public static void mockGet2Prompts(
            HttpClient mockedClient,
            PromptTemplate... templates
    ) throws Exception {

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

        when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                .thenReturn(response(200, JSONUtil.asString(payload)));
    }

    public static void mockOpenAITextCall(HttpClient mockedClient, String completion) throws Exception {
        when(request(mockedClient, "api.openai.com", "POST", "v1/completions"))
                .thenReturn(
                        response(200, getOpenAITextResponse(MODEL_TEXT_DAVINCI_003, completion)));
    }

    public static void mockOpenAIChatCall(HttpClient mockedClient, String completion) throws Exception {
        when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                .thenReturn(
                        response(200, getOpenAIChatResponse(completion)));
    }

    public static void mockUnauthorizedCreateSession(HttpClient mockedClient) throws Exception {
        when(request(mockedClient, "POST", "projects/[^/]*/sessions"))
                .thenReturn(response(401, ""));
    }

    public static void mockUnauthorizedGetPrompts(HttpClient mockedClient) throws Exception {
        when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                .thenReturn(response(401, ""));
    }

    public static void mockUnauthorizedOpenAITextCall(HttpClient mockedClient) throws Exception {
        when(request(mockedClient, "api.openai.com", "POST", "v1/completions"))
                .thenReturn(response(401, getOpenAIUnauthorizedResponse()));
    }

    public static void mockUnauthorizedOpenAIChatCall(HttpClient mockedClient) throws Exception {
        when(request(mockedClient, "api.openai.com", "POST", "v1/chat/completions"))
                .thenReturn(response(401, getOpenAIUnauthorizedResponse()));
    }

    public static String getSessionRequestPayload(String sessionId) {
        return JSONUtil.asString(
                object(
                        "session_id", sessionId
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

    public static String getOpenAITextResponse(String model, String response) {
        return "{\n" +
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

    private static List<Object> array(Object... objects) {
        return Arrays.asList(objects);
    }

    public static String escapeJSON(String jsonString) {
        return jsonString.replace("\"", "\\\"");
    }

    public static String unescapeExpected(String completion1) {
        return completion1.replace("\\n", "\n");
    }
}
