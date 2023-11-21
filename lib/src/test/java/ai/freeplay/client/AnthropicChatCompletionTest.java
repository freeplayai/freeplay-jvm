package ai.freeplay.client;

import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.Flavor;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.ChatStart;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.IndexedChatMessage;
import ai.freeplay.client.processor.PromptProcessor;
import org.junit.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedBodyAsMap;
import static ai.freeplay.client.internal.utilities.MockMethods.getCapturedRequest;
import static ai.freeplay.client.internal.utilities.PromptProcessors.testChatProcessor;
import static java.lang.String.format;
import static org.junit.Assert.*;

public class AnthropicChatCompletionTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";

    private final String chatPromptContent = "[{\"role\": \"user\", \"content\": \"Answer this question: {{question}}\"}]";
    private final String textCompletion = " I apologize that your sink isn't working. Can I help you";

    @Test
    public void chatCompletionReturnsValue() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
            mockAnthropicTextCall(mockedClient, textCompletion);

            CompletionResponse completion;

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
            //noinspection unchecked
            completion = fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    ),
                    "latest",
                    Flavor.DEFAULT,
                    PromptProcessor.DEFAULT,
                    Map.of("customer_id", 123)
            );

            Map<String, Object> sessionBody = getCapturedBodyAsMap(mockedClient, 4, 0);

            assertEquals(
                    Map.of("customer_id", 123),
                    sessionBody.get("metadata"));

            // Completion
            assertEquals(textCompletion, completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));
            assertEquals("[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"Human\"}]", recordBodyMap.get("prompt_content"));
            assertEquals(" I apologize that your sink isn't working. Can I help you", recordBodyMap.get("return_content"));
            assertNull(recordBodyMap.get("test_run_id"));
        });
    }

    @Test
    public void chatCompletionHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
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
                    testChatProcessor
            );

            // Modified Anthropic call
            Map<String, Object> anthropicRequestBody = getCapturedBodyAsMap(mockedClient, 4, 2);
            assertEquals(
                    "\n\nHuman: Answer this question: why isn't my sink working?\n" +
                            "\n" +
                            "Human: Inserted Message\n" +
                            "\n" +
                            "Assistant:",
                    anthropicRequestBody.get("prompt"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals(
                    "[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"Human\"},{\"content\":\"Inserted Message\",\"role\":\"user\"}]",
                    recordBodyMap.get("prompt_content"));
        });
    }

    @Test
    public void requiresRequiredParams() {
        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
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
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
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
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
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
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
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

    @Test
    public void chatCompletionStream() {
        String templateName = "my-prompt";

        withMockedClient((HttpClient mockedClient) -> {
            mockCreateSession(mockedClient);
            mockGetPrompts(mockedClient, templateName, chatPromptContent, Collections.emptyMap(), "anthropic_chat");
            mockAnthropicTextCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(MockFixtures.freeplayApiKey, baseUrl, new AnthropicProviderConfig(anthropicApiKey));
            ChatStart<Stream<IndexedChatMessage>> responseStream = fpClient.startChatStream(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens_to_sample", 64
                    ),
                    "latest"
            );

            List<IndexedChatMessage> chunks = responseStream.getFirstCompletion().collect(Collectors.toList());

            // Completion
            assertEquals(4, chunks.size());
            assertEquals(" Oh", chunks.get(0).getContent());
            assertEquals(" dear", chunks.get(1).getContent());
            assertEquals(",", chunks.get(2).getContent());
            assertEquals(" really", chunks.get(3).getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 4, 3);
            assertEquals("[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"Human\"}]", recordBodyMap.get("prompt_content"));
            assertEquals(" Oh dear, really", recordBodyMap.get("return_content"));
        });
    }
}
