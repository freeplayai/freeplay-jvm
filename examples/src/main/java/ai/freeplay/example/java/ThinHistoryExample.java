package ai.freeplay.example.java;

import ai.freeplay.client.Freeplay;
import ai.freeplay.client.resources.prompts.ChatMessage;
import ai.freeplay.client.resources.prompts.FormattedPrompt;
import ai.freeplay.client.resources.prompts.TemplatePrompt;
import ai.freeplay.client.resources.recordings.CallInfo;
import ai.freeplay.client.resources.recordings.RecordInfo;
import ai.freeplay.client.resources.recordings.ResponseInfo;
import ai.freeplay.client.resources.sessions.SessionInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ai.freeplay.client.Freeplay.Config;
import static ai.freeplay.example.java.ExampleUtils.callAnthropic;
import static java.lang.String.format;

/**
 * Demonstrates multi-turn conversation history with the thin client.
 *
 * The thin client never manages conversation state itself — you build and pass
 * the history list explicitly each turn. The prompt template must contain a
 * history placeholder (configured in the Freeplay UI) for bind() to inject it.
 */
public class ThinHistoryExample {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String freeplayApiKey = System.getenv("FREEPLAY_API_KEY");
        String projectId = System.getenv("FREEPLAY_PROJECT_ID");
        String baseUrl = format("%s/api", System.getenv("FREEPLAY_API_URL"));
        String anthropicApiKey = System.getenv("ANTHROPIC_API_KEY");

        Freeplay fpClient = new Freeplay(Config()
                .freeplayAPIKey(freeplayApiKey)
                .baseUrl(baseUrl)
        );

        List<String> questions = List.of(
                "who was the first president of the united states?",
                "what color is the sky?",
                "what shape is the earth?",
                "repeat the first question and answer"
        );
        List<String> articles = List.of(
                "george washington was the first president of the united states",
                "the sky is blue",
                "the earth is round",
                ""
        );

        // Fetch the template once; bind variables + history on each turn.
        TemplatePrompt template = fpClient.prompts()
                .get(projectId, "History-QA", "latest")
                .get();

        SessionInfo sessionInfo = fpClient.sessions().create()
                .customMetadata(Map.of("custom_field", "custom_value"))
                .getSessionInfo();

        List<ChatMessage> history = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> variables = Map.of(
                    "question", questions.get(i),
                    "article", articles.get(i)
            );
            System.out.println("variables: " + variables);

            // Pass accumulated history so the model sees previous turns.
            FormattedPrompt<List<ChatMessage>> formatted = template
                    .bind(new TemplatePrompt.BindRequest(variables).history(history))
                    .<List<ChatMessage>>format();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = callAnthropic(
                    objectMapper,
                    anthropicApiKey,
                    formatted.getPromptInfo().getModel(),
                    formatted.getPromptInfo().getModelParameters(),
                    formatted.getFormattedPrompt(),
                    formatted.getSystemContent().orElse(null)
            ).get();

            JsonNode bodyNode = objectMapper.readTree(response.body());
            String completion = bodyNode.path("content").get(0).path("text").asText();
            System.out.println("Completion: " + completion);

            List<ChatMessage> allMessages = formatted.allMessages(
                    new ChatMessage("assistant", completion)
            );

            // Append the last user + assistant turn to history for the next iteration.
            if (allMessages.size() >= 2) {
                history.add(allMessages.get(allMessages.size() - 2));
                history.add(allMessages.get(allMessages.size() - 1));
            } else {
                history.addAll(allMessages);
            }

            CallInfo callInfo = CallInfo.from(
                    formatted.getPromptInfo(),
                    startTime,
                    System.currentTimeMillis()
            );
            ResponseInfo responseInfo = new ResponseInfo(
                    "stop_sequence".equals(bodyNode.path("stop_reason").asText())
            );

            fpClient.recordings().create(
                    new RecordInfo(projectId, allMessages)
                            .inputs(variables)
                            .sessionInfo(sessionInfo)
                            .promptVersionInfo(formatted.getPromptInfo())
                            .callInfo(callInfo)
                            .responseInfo(responseInfo)
            ).get();
        }
    }
}
