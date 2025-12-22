package ai.freeplay.client.internal.utilities;

import ai.freeplay.client.internal.JSONUtil;
import ai.freeplay.client.model.PromptTemplate;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static java.lang.String.valueOf;
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

    public static final String promptTemplateId = UUID.randomUUID().toString();
    public static final String promptTemplateVersionId = UUID.randomUUID().toString();

    // Freeplay
    public static void mockRecord(
            HttpClient mockedClient
    ) throws RuntimeException {
        try {
            when(request(mockedClient, "POST", "v2/projects/[^/]*/sessions/[^/]*/completions"))
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

    public static void mockGetPromptsV2(
            HttpClient mockedClient,
            String templateName,
            List<Object> content,
            Map<String, Object> llmParameters,
            String flavor
    ) throws RuntimeException {
        try {
            when(request(mockedClient, "GET", "v2/projects/[^/]*/prompt-templates"))
                    .thenReturn(
                            response(
                                    200,
                                    getPromptsPayloadV2(
                                            flavor,
                                            promptTemplateId,
                                            promptTemplateVersionId,
                                            templateName,
                                            content,
                                            llmParameters)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockGetPromptV2Async(
            HttpClient mockedClient,
            String templateName,
            String environment,
            List<Object> content,
            Map<String, Object> llmParameters,
            String flavor
    ) throws RuntimeException {
        try {
            String matchUrl = String.format("v2/projects/[^/]*/prompt-templates/name/%s\\?environment=%s", templateName, environment);
            when(requestAsync(mockedClient, "GET", matchUrl)).thenReturn(
                    asyncResponse(
                            200,
                            getPromptPayloadV2(
                                    flavor,
                                    promptTemplateId,
                                    promptTemplateVersionId,
                                    templateName,
                                    content,
                                    llmParameters)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockGet2PromptsV2(
            HttpClient mockedClient,
            PromptTemplate... templates
    ) throws RuntimeException {
        List<Map<String, Object>> templateObjects = stream(templates).map((PromptTemplate template) -> {
            Map<String, Object> params = new HashMap<>(template.getLLMParameters());
            String model = String.valueOf(params.remove("model"));
            return object(
                    "prompt_template_id", template.getPromptTemplateId(),
                    "prompt_template_version_id", template.getPromptTemplateVersionId(),
                    "prompt_template_name", template.getName(),
                    "metadata", object(
                            "model", model,
                            "flavor", template.getFlavorName(),
                            "params", params
                    ),
                    "content", JSONUtil.parseList(template.getContent())
            );
        }).collect(toList());

        Map<String, Object> payload = object(
                "prompt_templates", array(
                        (Object[]) templateObjects.toArray(new Map[]{})
                )
        );

        try {
            when(request(mockedClient, "GET", "v2/projects/[^/]*/prompt-templates"))
                    .thenReturn(response(200, JSONUtil.asString(payload)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockRecordAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/sessions/[^/]*/completions")).thenReturn(
                    asyncResponse(
                            201,
                            getRecordPayloadWithCompletionId()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockRecordTraceAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/sessions/[^/]*/traces/id/[^/]*")).thenReturn(
                    asyncResponse(
                            201,
                            JSONUtil.asString(object())
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void mockRecordNoCompletionIdAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/sessions/[^/]*/completions")).thenReturn(
                    asyncResponse(
                            201,
                            JSONUtil.asString("")
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedRecordAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/sessions/[^/]*/completions"))
                    .thenReturn(asyncResponse(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockCreateTestRun(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "POST", "v2/projects/[^/]*/test-runs"))
                    .thenReturn(
                            response(201, getTestRunResponsePayload(UUID.randomUUID().toString())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockCreateTestRunAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/test-runs"))
                    .thenReturn(
                            asyncResponse(201, getTestRunTestCasesResponsePayload(UUID.randomUUID().toString(), true)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUpdateCustomerFeedbackAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/completion-feedback/id/[^/]*"))
                    .thenReturn(asyncResponse(201, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUpdateTraceFeedbackAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/trace-feedback/id/[^/]*"))
                    .thenReturn(asyncResponse(201, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUpdateSessionMetadataAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/id/[^/]*/metadata"))
                    .thenReturn(asyncResponse(200, "{\"message\": \"Metadata updated successfully\"}"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUpdateTraceMetadataAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/[^/]*/traces/id/[^/]*/metadata"))
                    .thenReturn(asyncResponse(200, "{\"message\": \"Metadata updated successfully\"}"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedUpdateSessionMetadataAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "PATCH", "v2/projects/[^/]*/sessions/id/[^/]*/metadata"))
                    .thenReturn(asyncResponse(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockSessionDelete(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "DELETE", "v2/projects/[^/]*/sessions/[^/]*"))
                    .thenReturn(asyncResponse(201, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockGetTestRunResults(HttpClient mockedClient, String testRunId) {
        String body = JSONUtil.asString(
                object(
                        "id", testRunId,
                        "name", null,
                        "description", null,
                        "summary_statistics", object(
                                "human_evaluation", object(),
                                "auto_evaluation", object()
                        )
                )
        );
        try {
            when(requestAsync(mockedClient, "GET", "v2/projects/[^/]*/test-runs/id/[^/]*"))
                    .thenReturn(asyncResponse(200, body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedUpdateCustomerFeedbackAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/completion-feedback/id/[^/]*"))
                    .thenReturn(asyncResponse(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedCreateTestRunAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "POST", "v2/projects/[^/]*/test-runs"))
                    .thenReturn(asyncResponse(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedGetPrompts(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "GET", "v2/projects/[^/]*/prompt-templates"))
                    .thenReturn(response(401, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedGetPromptsV2Async(HttpClient mockedClient) throws RuntimeException {
        try {
            when(requestAsync(mockedClient, "GET", "v2/projects/[^/]*/prompt-templates"))
                    .thenReturn(asyncResponse(401, ""));
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
            when(request(mockedClient, "api.anthropic.com", "POST", "v1/messages"))
                    .thenReturn(
                            response(200, getAnthropicResponse(MODEL_CLAUDE_2, completion)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockAnthropicCallStream(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.anthropic.com", "POST", "v1/messages"))
                    .thenReturn(
                            response(200, getAnthropicResponseStreamMessages()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUnauthorizedAnthropicCall(HttpClient mockedClient) throws RuntimeException {
        try {
            when(request(mockedClient, "api.anthropic.com", "POST", "v1/messages"))
                    .thenReturn(response(401, getAnthropicUnauthorizedResponse()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public static String getTestRunTestCasesResponsePayload(String testRunId, boolean includeOutputs) {
        return JSONUtil.asString(
                object(
                        "test_run_id", testRunId,
                        "test_cases", array(
                                object(
                                        "test_case_id", UUID.randomUUID(),
                                        "variables", object(
                                                "question", "Why isn't my sink working?"
                                        ),
                                        "output", includeOutputs ? "It took PTO today" : null
                                ),
                                object(
                                        "test_case_id", UUID.randomUUID(),
                                        "variables", object(
                                                "question", "Why isn't my internet working?"
                                        ),
                                        "output", includeOutputs ? "It's playing golf with the sink" : null
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

    public static List<Object> getChatPromptContentObjects() {
        return array(
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
        );
    }

    public static List<Object> getChatWithHistoryPromptContentObjects() {
        return array(
                object(
                        "role", "system",
                        "content", "You are a support agent."
                ),
                object(
                        "kind", "history"
                ),
                object(
                        "role", "user",
                        "content", "User message {{number}}"
                )
        );
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
                "  \"content\": [\n" +
                "    {\n" +
                "      \"text\": \"" + response + "\",\n" +
                "      \"type\": \"text\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"id\": \"msg_013Zva2CMHLNnXjNJJKqJ2EF\",\n" +
                "  \"model\": \"" + model + "\",\n" +
                "  \"role\": \"assistant\",\n" +
                "  \"stop_reason\": \"end_turn\",\n" +
                "  \"stop_sequence\": null,\n" +
                "  \"type\": \"message\",\n" +
                "  \"usage\": {\n" +
                "    \"input_tokens\": 10,\n" +
                "    \"output_tokens\": 25\n" +
                "  }\n" +
                "}";
    }

    public static Stream<String> getAnthropicResponseStreamMessages() {
        return Stream.of(
                "event: message_start",
                "data: {\"type\": \"message_start\", \"message\": {\"id\": \"msg_1nZdL29xx5MUA1yADyHTEsnR8uuvGzszyY\", \"type\": \"message\", \"role\": \"assistant\", \"content\": [], \"model\": \"claude-3-opus-20240229\", \"stop_reason\": null, \"stop_sequence\": null, \"usage\": {\"input_tokens\": 25, \"output_tokens\": 1}}}\n",
                "",
                "event: content_block_start",
                "data: {\"type\": \"content_block_start\", \"index\": 0, \"content_block\": {\"type\": \"text\", \"text\": \"\"}}\n",
                "",
                "event: ping",
                "data: {\"type\": \"ping\"}\n",
                "",
                "event: content_block_delta",
                "data: {\"type\": \"content_block_delta\", \"index\": 0, \"delta\": {\"type\": \"text_delta\", \"text\": \"Oh\"}}\n",
                "",
                "event: content_block_delta",
                "data: {\"type\": \"content_block_delta\", \"index\": 0, \"delta\": {\"type\": \"text_delta\", \"text\": \" dear\"}}\n",
                "",
                "event: content_block_delta",
                "data: {\"type\": \"content_block_delta\", \"index\": 0, \"delta\": {\"type\": \"text_delta\", \"text\": \",\"}}\n",
                "",
                "event: content_block_delta",
                "data: {\"type\": \"content_block_delta\", \"index\": 0, \"delta\": {\"type\": \"text_delta\", \"text\": \" really\"}}\n",
                "",
                "event: new-unknown-event",
                "data: ",
                "",
                "event: message_delta",
                "data: {\"type\": \"message_delta\", \"delta\": {\"stop_reason\": \"end_turn\", \"stop_sequence\":null, \"usage\":{\"output_tokens\": 15}}}",
                "",
                "event: message_stop",
                "data: {\"type\": \"message_stop\"}\n",
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
            object.put(valueOf(keysAndValues[i]), keysAndValues[i + 1]);
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


    @SuppressWarnings("SameParameterValue")
    private static String getPromptsPayloadV2(
            String flavorName,
            String promptTemplateId,
            String promptTemplateVersionId,
            String templateName,
            List<Object> content,
            Map<String, Object> llmParameters
    ) {
        HashMap<String, Object> params = new HashMap<>(llmParameters);
        String model = params.containsKey("model") ? valueOf(params.remove("model")) : MODEL_GPT_35_TURBO;
        return JSONUtil.asString(object(
                "prompt_templates", array(
                        object(
                                "prompt_template_id", promptTemplateId,
                                "prompt_template_version_id", promptTemplateVersionId,
                                "prompt_template_name", templateName,
                                "content", content,
                                "format_version", 2,
                                "metadata", object(
                                        "provider", "openai",
                                        "model", model,
                                        "flavor", flavorName,
                                        "params", params,
                                        "provider_info", Map.of("provider", "info")
                                )
                        )
                )
        ));
    }

    @SuppressWarnings("SameParameterValue")
    private static String getPromptPayloadV2(
            String flavorName,
            String promptTemplateId,
            String promptTemplateVersionId,
            String templateName,
            List<Object> content,
            Map<String, Object> llmParameters
    ) {
        HashMap<String, Object> params = new HashMap<>(llmParameters);
        String model = params.containsKey("model") ? valueOf(params.get("model")) : MODEL_GPT_35_TURBO;
        return JSONUtil.asString(
                object(
                        "prompt_template_id", promptTemplateId,
                        "prompt_template_version_id", promptTemplateVersionId,
                        "prompt_template_name", templateName,
                        "content", content,
                        "format_version", 2,
                        "metadata", object(
                                "provider", "openai",
                                "model", model,
                                "flavor", flavorName,
                                "params", params,
                                "provider_info", Map.of("provider", "info")
                        )
                )
        );
    }

    private static String getRecordPayloadWithCompletionId() {
        return JSONUtil.asString(object(
                "completion_id", UUID.randomUUID().toString()
        ));
    }

    public static void mockCreatePromptVersionAsync(
            HttpClient mockedClient,
            String promptTemplateName,
            String promptTemplateId,
            String promptTemplateVersionId
    ) throws RuntimeException {
        try {
            String matchUrl = String.format("v2/projects/[^/]*/prompt-templates/name/%s/versions", promptTemplateName);
            when(requestAsync(mockedClient, "POST", matchUrl)).thenReturn(
                    asyncResponse(
                            201,
                            getCreatePromptVersionResponsePayload(
                                    promptTemplateId,
                                    promptTemplateVersionId,
                                    promptTemplateName
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getCreatePromptVersionResponsePayload(
            String promptTemplateId,
            String promptTemplateVersionId,
            String promptTemplateName
    ) {
        return JSONUtil.asString(object(
                "prompt_template_id", promptTemplateId,
                "prompt_template_version_id", promptTemplateVersionId,
                "prompt_template_name", promptTemplateName,
                "version_name", "v1.0",
                "version_description", "Test version",
                "metadata", object(
                        "provider", "anthropic",
                        "model", "claude-3-5-sonnet-20241022",
                        "flavor", "anthropic_chat",
                        "params", object("temperature", 0.7),
                        "provider_info", object()
                ),
                "format_version", 2,
                "project_id", projectId,
                "content", array(
                        object(
                                "role", "user",
                                "content", "Answer this question as concisely as you can: {{question}}"
                        )
                ),
                "tool_schema", array()
        ));
    }

    public static void mockUpdateVersionEnvironmentsAsync(HttpClient mockedClient) throws RuntimeException {
        try {
            String matchUrl = "v2/projects/[^/]*/prompt-templates/id/[^/]*/versions/[^/]*/environments";
            when(requestAsync(mockedClient, "POST", matchUrl)).thenReturn(
                    asyncResponse(200, "")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockUpdateVersionEnvironmentsAsyncUnauthorized(HttpClient mockedClient) throws RuntimeException {
        try {
            String matchUrl = "v2/projects/[^/]*/prompt-templates/id/[^/]*/versions/[^/]*/environments";
            when(requestAsync(mockedClient, "POST", matchUrl)).thenReturn(
                    asyncResponse(400, JSONUtil.asString(object("message", "Project not found")))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
