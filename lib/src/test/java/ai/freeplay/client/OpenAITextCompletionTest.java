package ai.freeplay.client;

import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.CompletionResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;

import static ai.freeplay.client.ProviderConfig.OpenAIProviderConfig;
import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAITextCompletionTest {

    private final String name = "my-prompt";
    private final String content = "Answer this question: {{question}}";

    private HttpClient mockedClient;

    @Before
    public void beforeEach() {
        mockedClient = mock(HttpClient.class);
    }

    @Test
    public void textCompletionReturnsValue() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, name, content, Collections.emptyMap(), "openai_text");
        mockCallOpenAI();

        CompletionResponse completion;

        try (MockedStatic<HttpClient> httpClientClass = Mockito.mockStatic(HttpClient.class)) {
            httpClientClass.when(HttpClient::newHttpClient).thenReturn(mockedClient);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new OpenAIProviderConfig(openaiApiKey));
            completion = fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of("model", MODEL_TEXT_DAVINCI_003),
                    "latest"
            );

            // Completion
            String textCompletionExpected = "\n\nI apologize that your sink isn't working. Can I help you";
            assertEquals(textCompletionExpected, completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));
            assertEquals("Answer this question: why isn't my sink working?", recordBodyMap.get("prompt_content"));
            assertEquals("\n\nI apologize that your sink isn't working. Can I help you", recordBodyMap.get("return_content"));
            assertNull(recordBodyMap.get("test_run_id"));
        }
    }

    @Test
    public void requiresModelParam() throws Exception {
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, name, content, Collections.emptyMap(), "openai_text");

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
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, name, content, Collections.emptyMap(), "openai_text");

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
        mockUnauthorizedCreateSession(mockedClient);

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
        mockCreateSession(mockedClient);
        mockUnauthorizedGetPrompts(mockedClient);

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
        mockCreateSession(mockedClient);
        mockGetPrompts(mockedClient, name, content, Collections.emptyMap(), "openai_text");
        mockUnauthorizedOpenAITextCall(mockedClient);

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
        mockCreateSession(mockedClient);
        mockGetPrompts(
                mockedClient,
                name,
                content,
                Map.of(
                        "model", "gpt-turbo-3.5",
                        "max_tokens", "11",
                        "temperature", "0.22"
                ), "openai_text"
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
    }

    private void mockCallOpenAI() throws Exception {
        String textCompletion = "\\n\\nI apologize that your sink isn't working. Can I help you";
        when(request(mockedClient, "api.openai.com", "POST", "v1/completions"))
                .thenReturn(
                        response(200, getOpenAITextResponse(MODEL_TEXT_DAVINCI_003, textCompletion)));
    }
}
