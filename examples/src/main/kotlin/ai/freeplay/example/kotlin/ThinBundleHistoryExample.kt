package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.FilesystemTemplateResolver
import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.prompts.TemplatePrompt
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import ai.freeplay.example.java.ThinExampleUtils.callAnthropic
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

private val objectMapper = ObjectMapper()

fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")
    val templateDirectory = System.getenv("FREEPLAY_TEMPLATE_DIRECTORY")

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
            .templateResolver(FilesystemTemplateResolver(Paths.get(templateDirectory)))
    )

    val variables = mapOf("question" to "Why isn't my window working?")
    val history = listOf(
        ChatMessage("user", "Entertain me"),
        ChatMessage("assistant", "Okay"),
    )

    println("Getting the prompt...")
    val template = fpClient.prompts()
        .get(
            projectId,
            "my-anthropic-history-prompt",
            "latest"
        ).await()
    val formatted = template.bind(TemplatePrompt.BindRequest(variables).history(history)).format<List<ChatMessage>>()

    println("Calling Anthropic...")
    val startTime = System.currentTimeMillis()
    val llmResponse = callAnthropic(
        objectMapper,
        anthropicApiKey,
        formatted.promptInfo.model,
        formatted.promptInfo.modelParameters,
        formatted.formattedPrompt,
        formatted.systemContent.orElse(null)
    ).await()

    val bodyNode = objectMapper.readTree(llmResponse.body())
    println("Completion: " + bodyNode.path("content").get(0).path("text").asText())

    println("Recording the result")
    val allMessages: List<ChatMessage> = formatted.allMessages(
        ChatMessage("assistant", bodyNode.path("content").get(0).path("text").asText())
    )
    val callInfo = CallInfo.from(
        formatted.promptInfo,
        startTime,
        System.currentTimeMillis()
    )
    val responseInfo = ResponseInfo("stop_sequence" == bodyNode.path("stop_reason").asText())
    val sessionInfo = fpClient.sessions().create()
        .customMetadata(mapOf("custom_field" to "custom_value"))
        .sessionInfo

    val recordResponse = fpClient.recordings().create(
        RecordInfo(
            projectId,
            allMessages
        ).inputs(variables)
            .sessionInfo(sessionInfo)
            .promptVersionInfo(formatted.getPromptInfo())
            .callInfo(callInfo)
            .responseInfo(responseInfo)
    ).await()
    println("Recorded with completionId ${recordResponse.completionId}")
}
