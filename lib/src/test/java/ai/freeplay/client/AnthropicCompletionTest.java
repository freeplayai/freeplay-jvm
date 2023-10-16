package ai.freeplay.client;

import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.CompletionResponse;
import org.junit.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedRequest;
import static ai.freeplay.client.internal.utilities.PromptProcessors.testTextProcessor;
import static java.lang.String.format;
import static org.junit.Assert.*;

public class AnthropicCompletionTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";

    private final String textPromptContent = "Answer this question: {{question}}";
    private final String textCompletion = " I apologize that your sink isn't working. Can I help you";

    @Test
    public void textCompletionReturnsValue() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCall(mockedClient, textCompletion);

            CompletionResponse completion;

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
            completion = fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    ),
                    "latest"
            );

            // Completion
            assertEquals(textCompletion, completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));
            assertEquals("Answer this question: why isn't my sink working?", recordBodyMap.get("prompt_content"));
            assertEquals(" I apologize that your sink isn't working. Can I help you", recordBodyMap.get("return_content"));
            assertNull(recordBodyMap.get("test_run_id"));
        });
    }

    @Test
    public void textCompletionHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCall(mockedClient, textCompletion);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    ),
                    "latest",
                    testTextProcessor
            );

            // Modified Anthropic call
            Map<String, Object> anthropicRequestBody = getCapturedBodyAsMap(mockedClient, 4, 2);
            assertEquals(
                    "\n\nHuman: PREPENDED_TEXT Answer this question: why isn't my sink working? \n\nAssistant:",
                    anthropicRequestBody.get("prompt"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(
                    "PREPENDED_TEXT Answer this question: why isn't my sink working?",
                    recordBodyMap.get("prompt_content"));
        });
    }

    @Test
    public void requiresRequiredParams() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCall(mockedClient, textCompletion);

            Stream<String> requiredParameters = Stream.of("model", "max_tokens_to_sample");

            Map<String, Object> llmParameters = Map.of(
                    "model", MODEL_CLAUDE_2,
                    "max_tokens_to_sample", 64
            );

            requiredParameters.forEach((String param) -> {
                Map<String, Object> requestLLMParameters = new HashMap<>(llmParameters);
                Object removed = requestLLMParameters.remove(param);
                assertNotNull("Attempting to verify a missing param that wasn't in the map.", removed);

                try {
                    Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
                    fpClient.getCompletion(
                            projectId,
                            "my-prompt",
                            Map.of("question", "why isn't my sink working?"),
                            requestLLMParameters,
                            "latest"
                    );
                    fail(format("Should have gotten an exception requiring the %s parameter", param));
                } catch (FreeplayException fpe) {
                    assertEquals(format("The '%s' parameter is required when calling Anthropic.", param), fpe.getMessage());
                }
            });
        });
    }

    @Test
    public void disallowsPromptParam() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCall(mockedClient, textCompletion);

            try {
                Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_CLAUDE_2,
                                "max_tokens_to_sample", 64,
                                "prompt", "this is not allowed"
                        ),
                        "latest"
                );
                fail("Should have gotten an exception disallowing the 'prompt' parameter");
            } catch (FreeplayException fpe) {
                assertEquals("The 'prompt' parameter cannot be specified. It is populated automatically.", fpe.getMessage());
            }
        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void sendsRequiredHeaders() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockAnthropicTextCall(mockedClient, textCompletion);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new AnthropicProviderConfig(anthropicApiKey));

            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    ),
                    "latest"
            );

            // Verify headers were sent
            HttpRequest anthropicRequest = getCapturedRequest(mockedClient, 4, 2);
            assertEquals("application/json", anthropicRequest.headers().firstValue("accept").get());
            assertEquals("application/json", anthropicRequest.headers().firstValue("content-type").get());
            assertEquals("2023-06-01", anthropicRequest.headers().firstValue("anthropic-version").get());
            assertEquals(anthropicApiKey, anthropicRequest.headers().firstValue("x-api-key").get());
        });
    }

    @Test
    public void handlesUnauthorizedCallingAnthropic() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, textPromptContent, Collections.emptyMap(), "anthropic_text");
            mockUnauthorizedAnthropicTextCall(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(
                        MockFixtures.freeplayApiKey,
                        baseUrl,
                        new AnthropicProviderConfig("not-real-key"));

                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_CLAUDE_2,
                                "max_tokens_to_sample", 64
                        ),
                        "latest"
                );
                fail("Should have gotten an exception for a 401");
            } catch (FreeplayException fpe) {
                assertEquals("Error making call [401]", fpe.getMessage());
            }
        });
    }
}
