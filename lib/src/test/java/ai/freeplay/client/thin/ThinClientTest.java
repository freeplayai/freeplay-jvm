package ai.freeplay.client.thin;

import ai.freeplay.client.HttpClientTestBase;
import ai.freeplay.client.exceptions.FreeplayConfigurationException;
import org.junit.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ai.freeplay.client.internal.utilities.MockFixtures.*;
import static ai.freeplay.client.thin.Freeplay.Config;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ThinClientTest extends HttpClientTestBase {

    private final String templateName = "my-prompt";

    @Test
    public void testGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            Map<String, Object> llmParameters = Map.of("model", MODEL_GPT_35_TURBO);

            mockGetPromptsAsync(mockedClient, templateName, getChatPromptContent(), llmParameters, "openai_chat");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();

            PromptInfo expectedInfo = new PromptInfo(
                    promptTemplateId,
                    promptTemplateVersionId,
                    templateName,
                    "prod",
                    Map.of(),
                    "openai",
                    MODEL_GPT_35_TURBO,
                    "openai_chat"
            );
            assertEquals(expectedInfo, templatePrompt.getPromptInfo());

            List<ChatMessage> expectedMessages = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("assistant", "How may I help you?"),
                    new ChatMessage("user", "{{question}}")
            );
            assertEquals(expectedMessages, templatePrompt.getMessages());
        });
    }

    @Test
    public void testSyntax() {
        withMockedClient((HttpClient mockedClient) -> {
            Map<String, Object> llmParameters = Map.of(
                    "model", MODEL_CLAUDE_2,
                    "max_tokens", 256
            );
            Map<String, Object> variables = Map.of("question", "Why isn't my light working?");
            mockGetPromptsAsync(mockedClient, templateName, getChatPromptContent(), llmParameters, "anthropic_chat");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            String anthropicSyntax = templatePrompt
                    .bind(variables)
                    .format(templatePrompt.getPromptInfo().getFlavorName());

            assertEquals("\n\nHuman: You are a support agent." +
                            "\n\nAssistant: How may I help you?" +
                            "\n\nHuman: Why isn't my light working?" +
                            "\n\nAssistant:",
                    anthropicSyntax);

            // Overriding flavor_name

            List<ChatMessage> openAISyntax = templatePrompt
                    .bind(variables)
                    .format("openai_chat");

            List<ChatMessage> expectedMessages = List.of(
                    new ChatMessage("system", "You are a support agent."),
                    new ChatMessage("assistant", "How may I help you?"),
                    new ChatMessage("user", "Why isn't my light working?")
            );
            assertEquals(expectedMessages, openAISyntax);

            // Calling getFormatted instead of chaining manually
            CompletableFuture<FormattedPrompt<List<ChatMessage>>> formattedPrompt = fpClient.prompts().getFormatted(
                    projectId,
                    templateName,
                    "prod",
                    variables,
                    "openai_chat"
            );
            assertEquals(expectedMessages, formattedPrompt.get().getFormattedPrompt());
        });
    }

    @Test
    public void testInvalidFlavorName() {
        withMockedClient((HttpClient mockedClient) -> {
            Map<String, Object> llmParameters = Map.of(
                    "model", MODEL_CLAUDE_2,
                    "max_tokens", 256
            );
            Map<String, Object> variables = Map.of("question", "Why isn't my light working?");
            mockGetPromptsAsync(mockedClient, templateName, getChatPromptContent(), llmParameters, "anthropic_chat");

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            // Calling get() and chaining calls
            TemplatePrompt templatePrompt = fpClient.prompts().get(projectId, templateName, "prod").get();
            FreeplayConfigurationException exception = assertThrows(
                    FreeplayConfigurationException.class,
                    () -> templatePrompt
                            .bind(variables)
                            .format("not_a_flavor")
            );
            String expectedExceptionMessage = "Unable to create LLMAdapter for name 'not_a_flavor'.\n";
            assertEquals(expectedExceptionMessage, exception.getMessage());

            // Calling getFormatted()
            ExecutionException exception2 = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.prompts()
                            .getFormatted(projectId, templateName, "prod", Map.of(), "not_a_flavor")
                            .get()
            );
            assertEquals(expectedExceptionMessage, exception2.getCause().getMessage());
        });
    }

    @Test
    public void handlesUnauthorizedOnGetPrompts() {
        withMockedClient((HttpClient mockedClient) -> {
            mockUnauthorizedGetPromptsAsync(mockedClient);

            Freeplay fpClient = new Freeplay(Config().freeplayAPIKey(freeplayApiKey).baseUrl(baseUrl));

            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> fpClient.prompts().get(
                            projectId,
                            "my-prompt",
                            "latest"
                    ).get());
            assertEquals("Error making call [401]", exception.getCause().getMessage());
        });
    }
}
