package ai.freeplay.client;

import ai.freeplay.client.ProviderConfig.AnthropicProviderConfig;
import ai.freeplay.client.exceptions.FreeplayException;
import ai.freeplay.client.flavor.ChatFlavor;
import ai.freeplay.client.internal.utilities.MockFixtures;
import ai.freeplay.client.model.ChatStart;
import ai.freeplay.client.model.CompletionResponse;
import ai.freeplay.client.model.CompletionSession;
import ai.freeplay.client.model.IndexedChatMessage;
import ai.freeplay.client.processor.ChatPromptProcessor;
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

    private final List<Object> chatPromptContentObjects = array(
            object(
                    "role", "user",
                    "content", "Answer this question: {{question}}"
            )
    );
    private final List<Object> complexChatPromptContentObjects = array(
            object(
                    "role", "user",
                    "content", "Something {{#value}}{{name}}{{/value}}{{> post_run_messages}}"
            )
    );
    private final List<Object> complexListChatPromptContentObjects = array(
            object(
                    "role", "user",
                    "content", "Do these things: {{#tasks}}{{name}}{{/tasks}}"
            )
    );

    private final String completionContent = " I apologize that your sink isn't working. Can I help you";

    @Test
    public void chatCompletionReturnsValue() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat");
            mockAnthropicCall(mockedClient, completionContent);

            CompletionResponse completion;

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );
            completion = fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    "latest",
                    ChatFlavor.DEFAULT,
                    ChatPromptProcessor.DEFAULT,
                    Map.of("customer_id", 123)
            );

            // Completion
            assertEquals(completionContent, completion.getContent());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(promptTemplateVersionId, recordBodyMap.get("project_version_id"));
            assertEquals(promptTemplateId, recordBodyMap.get("prompt_template_id"));
            assertEquals("[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"user\"}]", recordBodyMap.get("prompt_content"));
            assertEquals(" I apologize that your sink isn't working. Can I help you", recordBodyMap.get("return_content"));
            assertNull(recordBodyMap.get("test_run_id"));
            assertEquals(Map.of("customer_id", 123), recordBodyMap.get("custom_metadata"));
        });
    }

    @Test
    public void complexChatCompletionReturnsValue() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(
                    mockedClient, templateName, complexChatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockAnthropicCall(mockedClient, completionContent);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("value",
                            Map.of("name", "this-is-a-name")
                    ),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    "latest"
            );

            Map<String, Object> anthropicCall = getCapturedBodyAsMap(mockedClient, 3, 1);
            assertEquals(
                    List.of(Map.of("content", "Something this-is-a-name", "role", "user")),
                    anthropicCall.get("messages"));
        });
    }

    @Test
    public void complexListChatCompletionReturnsValue() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(
                    mockedClient, templateName, complexListChatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockAnthropicCall(mockedClient, completionContent);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("tasks",
                            List.of(
                                    Map.of("name", "\ntask1"),
                                    Map.of("name", "\ntask2"),
                                    Map.of("name", "\ntask3")
                            )
                    ),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    "latest"
            );

            Map<String, Object> anthropicCall = getCapturedBodyAsMap(mockedClient, 3, 1);
            assertEquals(List.of(Map.of("content", "Do these things: \ntask1\ntask2\ntask3", "role", "user")),
                    anthropicCall.get("messages"));
        });
    }

    @Test
    public void chatCompletionHandlesProcessor() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(
                    mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockAnthropicCall(mockedClient, completionContent);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );
            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    "latest",
                    testChatProcessor
            );

            // Modified Anthropic call
            Map<String, Object> anthropicRequestBody = getCapturedBodyAsMap(mockedClient, 3, 1);

            assertEquals(List.of(
                            Map.of("content", "Answer this question: why isn't my sink working?", "role", "user"),
                            Map.of("content", "Inserted Message", "role", "user")),
                    anthropicRequestBody.get("messages"));


            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(
                    "[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"user\"},{\"content\":\"Inserted Message\",\"role\":\"user\"}]",
                    recordBodyMap.get("prompt_content"));
        });
    }

    @Test
    public void requiresRequiredParams() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat");
            mockAnthropicCall(mockedClient, completionContent);

            Stream<String> requiredParameters = Stream.of("max_tokens");

            Map<String, Object> llmParameters = Map.of(
                    "model", MODEL_CLAUDE_2,
                    "max_tokens", 64
            );

            requiredParameters.forEach((String param) -> {
                Map<String, Object> requestLLMParameters = new HashMap<>(llmParameters);
                Object removed = requestLLMParameters.remove(param);
                assertNotNull("Attempting to verify a missing param that wasn't in the map.", removed);

                try {
                    Freeplay fpClient = new Freeplay(
                            MockFixtures.freeplayApiKey,
                            baseUrl,
                            new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
                    );
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
            mockGetPromptsV2(mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat");
            mockAnthropicCall(mockedClient, completionContent);

            try {
                Freeplay fpClient = new Freeplay(
                        MockFixtures.freeplayApiKey,
                        baseUrl,
                        new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
                );
                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_CLAUDE_2,
                                "max_tokens", 64,
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
            mockGetPromptsV2(
                    mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockAnthropicCall(mockedClient, completionContent);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );

            fpClient.getCompletion(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    "latest"
            );

            // Verify headers were sent
            HttpRequest anthropicRequest = getCapturedRequest(mockedClient, 3, 1);
            assertEquals("application/json", anthropicRequest.headers().firstValue("accept").get());
            assertEquals("application/json", anthropicRequest.headers().firstValue("content-type").get());
            assertEquals("2023-06-01", anthropicRequest.headers().firstValue("anthropic-version").get());
            assertEquals(anthropicApiKey, anthropicRequest.headers().firstValue("x-api-key").get());
        });
    }

    @Test
    public void handlesUnauthorizedCallingAnthropic() {
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(
                    mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockUnauthorizedAnthropicCall(mockedClient);

            try {
                Freeplay fpClient = new Freeplay(
                        MockFixtures.freeplayApiKey,
                        baseUrl,
                        new ProviderConfigs(new AnthropicProviderConfig("not-real-key"))
                );

                fpClient.getCompletion(
                        projectId,
                        "my-prompt",
                        Map.of("question", "why isn't my sink working?"),
                        Map.of(
                                "model", MODEL_CLAUDE_2,
                                "max_tokens", 64
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
            mockGetPromptsV2(
                    mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockAnthropicCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );
            ChatStart<Stream<IndexedChatMessage>> responseStream = fpClient.startChatStream(
                    projectId,
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    "latest"
            );

            List<IndexedChatMessage> chunks = responseStream.getFirstCompletion().collect(Collectors.toList());

            // Completion
            assertEquals(5, chunks.size());
            assertEquals("Oh", chunks.get(0).getContent());
            assertEquals(" dear", chunks.get(1).getContent());
            assertEquals(",", chunks.get(2).getContent());
            assertEquals(" really", chunks.get(3).getContent());
            assertFalse(chunks.get(4).isComplete());

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals("[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"user\"}]", recordBodyMap.get("prompt_content"));
            assertEquals("Oh dear, really", recordBodyMap.get("return_content"));
        });
    }

    @Test
    public void chatCompletionStreamHandlesProcessor() {
        String templateName = "my-prompt";
        withMockedClient((HttpClient mockedClient) -> {
            mockGetPromptsV2(
                    mockedClient, templateName, chatPromptContentObjects, Collections.emptyMap(), "anthropic_chat"
            );
            mockAnthropicCallStream(mockedClient);

            Freeplay fpClient = new Freeplay(
                    MockFixtures.freeplayApiKey,
                    baseUrl,
                    new ProviderConfigs(new AnthropicProviderConfig(anthropicApiKey))
            );
            CompletionSession session = fpClient.createSession(projectId, "latest");
            Stream<IndexedChatMessage> responseStream = session.getCompletionStream(
                    "my-prompt",
                    Map.of("question", "why isn't my sink working?"),
                    Map.of(
                            "model", MODEL_CLAUDE_2,
                            "max_tokens", 64
                    ),
                    null,
                    testChatProcessor
            );

            @SuppressWarnings("unused")
            List<IndexedChatMessage> chunks = responseStream.collect(Collectors.toList());

            // Modified Anthropic call
            Map<String, Object> anthropicRequest = getCapturedBodyAsMap(mockedClient, 3, 1);
            assertEquals(List.of(
                            Map.of("content", "Answer this question: why isn't my sink working?", "role", "user"),
                            Map.of("content", "Inserted Message", "role", "user")),
                    anthropicRequest.get("messages"));

            // Record call
            Map<String, Object> recordBodyMap = getCapturedBodyAsMap(mockedClient, 3, 2);
            assertEquals(
                    "[{\"content\":\"Answer this question: why isn't my sink working?\",\"role\":\"user\"},{\"content\":\"Inserted Message\",\"role\":\"user\"}]",
                    recordBodyMap.get("prompt_content"));

        });
    }
}
