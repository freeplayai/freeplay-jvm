package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.model.CompletionResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static ai.freeplay.client.internal.utilities.MockPayloads.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FreeplayTest {

    private static final String baseUrl = "http://localhost:8080/api";

    public static final String MODEL_TEXT_DAVINCI_003 = "text-davinci-003";

    private final String freeplayApiKey = "<freeplay-api-key>";
    private final String openaiApiKey = "<openai-api-key>";
    private final String projectId = UUID.randomUUID().toString();

    private final String projectVersionId = UUID.randomUUID().toString();
    private final String promptTemplateId = UUID.randomUUID().toString();
    private final String promptTemplateVersionId = UUID.randomUUID().toString();
    private final String name = "my-prompt";
    private final String content = "Answer this question: {{question}}";
    private final String textCompletion = "\\n\\nI apologize that your sink isn't working. Can I help you";
    private final String textCompletionExpected = "\n\nI apologize that your sink isn't working. Can I help you";

    private HttpClient mockedClient;

    @Before
    public void beforeEach() {
        mockedClient = mock(HttpClient.class);
    }

    @Test
    public void textCompletionReturnsValue() throws Exception {
        mockCreateSession();
        mockGetPrompts();
        mockCallOpenAI();

        CompletionResponse completion;

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            completion = fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of("model", MODEL_TEXT_DAVINCI_003),
                    "latest"
            );
        }

        // Completion
        assertEquals(textCompletionExpected, completion.getContent());

        // Record call
        Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
        assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
        assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));
        assertEquals("Answer this question: why isn't my sink working?", recordBodyMap.get("prompt_content"));
        assertEquals("\n\nI apologize that your sink isn't working. Can I help you", recordBodyMap.get("return_content"));
        assertNull(recordBodyMap.get("test_run_id"));
    }

    @Test
    public void requiresModelParam() throws Exception {
        mockCreateSession();
        mockGetPrompts();

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Collections.emptyMap(),
                    "latest"
            );
            fail("Should have gotten an exception requiring the model parameter");
        } catch (FreeplayException fpe) {
            assertEquals("The 'model' parameter is required when calling OpenAI", fpe.getMessage());
        }
    }

    @Test
    public void disallowsPromptParam() throws Exception {
        mockCreateSession();
        mockGetPrompts();

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_TEXT_DAVINCI_003,
                            "prompt", "this is not allowed"
                    ),
                    "latest"
            );
            fail("Should have gotten an exception disallowing the prompt parameter");
        } catch (FreeplayException fpe) {
            assertEquals(
                    "The 'prompt' parameter cannot be specified. It is populated automatically.",
                    fpe.getMessage());
        }
    }

    @Test
    public void handlesUnauthorizedOnCreateSession() throws Exception {
        mockUnauthorizedCreateSession();

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    "latest"
            );
            fail("Should have gotten an exception for a 401");
        } catch (FreeplayException fpe) {
            assertEquals("Error calling Freeplay [401]", fpe.getMessage());
        }
    }

    @Test
    public void handlesUnauthorizedOnGetPrompts() throws Exception {
        mockCreateSession();
        mockUnauthorizedGetPrompts();

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    "latest"
            );
            fail("Should have gotten an exception for a 401");
        } catch (FreeplayException fpe) {
            assertEquals("Error calling Freeplay [401]", fpe.getMessage());
        }
    }

    @Test
    public void handlesUnauthorizedCallingOpenAI() throws Exception {
        mockCreateSession();
        mockGetPrompts();
        mockUnauthorizedOpenAICall();

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of("model", MODEL_TEXT_DAVINCI_003),
                    "latest"
            );
            fail("Should have gotten an exception for a 401");
        } catch (FreeplayException fpe) {
            assertEquals("Error making call [401]", fpe.getMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlesLLMParameterMergePrecedence() throws Exception {
        mockCreateSession();
        mockGetPrompts(Map.of(
                        "model", "gpt-turbo-3.5",
                        "max_tokens", "11",
                        "temperature", "0.22"
                )
        );
        mockCallOpenAI();

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Map<String, Object> clientParams = Map.of("max_tokens", "33");
            Map<String, Object> callParams = Map.of("temperature", "0.44");

            Freeplay fpClient = new Freeplay(
                    freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey), clientParams);
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    callParams,
                    "latest"
            );
        }

        Map<String, Object> openaiBody = getCapturedBodyAsMap(mockedClient, 4, 2);
        assertEquals("gpt-turbo-3.5", openaiBody.get("model"));
        assertEquals("33", openaiBody.get("max_tokens"));
        assertEquals("0.44", openaiBody.get("temperature"));

        Map<String, Object> recordBody = getCapturedBodyAsMap(mockedClient, 4, 3);
        Map<String, Object> recordedParameters = (Map<String, Object>) recordBody.get("llm_parameters");
        assertEquals("gpt-turbo-3.5", recordedParameters.get("model"));
        assertEquals("33", recordedParameters.get("max_tokens"));
        assertEquals("0.44", recordedParameters.get("temperature"));
    }

    private void mockCallOpenAI() throws Exception {
        when(request(mockedClient, "api.openai.com", "POST", "v1/completions"))
                .thenReturn(
                        response(200, getOpenAITextResponse(MODEL_TEXT_DAVINCI_003, textCompletion)));
    }

    private void mockGetPrompts() throws Exception {
        when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                .thenReturn(
                        response(200, getPromptsPayload(projectVersionId, promptTemplateId, promptTemplateVersionId, name, content)));
    }

    private void mockGetPrompts(Map<String, Object> llmParameters) throws Exception {
        when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                .thenReturn(
                        response(
                                200,
                                getPromptsPayload(projectVersionId, promptTemplateId, promptTemplateVersionId, name, content, llmParameters)));
    }

    private void mockCreateSession() throws Exception {
        when(request(mockedClient, "POST", "projects/[^/]*/sessions"))
                .thenReturn(
                        response(201, getSessionRequestPayload(UUID.randomUUID().toString())));
    }

    private void mockUnauthorizedCreateSession() throws Exception {
        when(request(mockedClient, "POST", "projects/[^/]*/sessions"))
                .thenReturn(response(401, ""));
    }

    private void mockUnauthorizedGetPrompts() throws Exception {
        when(request(mockedClient, "GET", "projects/[^/]*/templates"))
                .thenReturn(response(401, ""));
    }

    private void mockUnauthorizedOpenAICall() throws Exception {
        when(request(mockedClient, "api.openai.com", "POST", "v1/completions"))
                .thenReturn(response(401, getOpenAIUnauthorizedResponse()));
    }
}
