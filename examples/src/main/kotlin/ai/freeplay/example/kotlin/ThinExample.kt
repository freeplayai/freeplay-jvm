package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.prompts.Prompts
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import ai.freeplay.example.java.ThinExampleUtils.callAnthropic
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

private val objectMapper = ObjectMapper()

fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    )

    val variables = mapOf("question" to "Why isn't my window working?")

    println("Getting the prompt...")
    val prompt = fpClient
        .prompts()
        .getFormatted<List<ChatMessage>>(
            Prompts.GetFormattedRequest(
                projectId,
                "my-anthropic-prompt",
                "latest",
                variables
            )
        )
        .await()

    println("Calling Anthropic...")
    val startTime = System.currentTimeMillis()
    val llmResponse = callAnthropic(
        objectMapper,
        anthropicApiKey,
        prompt.promptInfo.model,
        prompt.promptInfo.modelParameters,
        prompt.formattedPrompt,
        prompt.systemContent.orElse(null)
    ).await()

    val endTime = System.currentTimeMillis()

    val bodyNode = objectMapper.readTree(llmResponse.body())
    println("Completion: " + bodyNode.path("content").get(0).path("text").asText())

    println("Recording the result")
    val allMessages: List<ChatMessage> = prompt.allMessages(
        ChatMessage("assistant", bodyNode.path("content").get(0).path("text").asText())
    )
    val callInfo = CallInfo.from(
        prompt.promptInfo,
        startTime,
        endTime
    )
    val responseInfo = ResponseInfo("stop_sequence" == bodyNode.path("stop_reason").asText())
    val session = fpClient.sessions().create()
        .customMetadata(mapOf("custom_field" to "custom_value"))

    val trace = session.createTrace("input str", "agent name", null)

    val recordResponse = fpClient.recordings().create(
        RecordInfo(
            projectId,
            allMessages
        ).inputs(variables)
            .sessionInfo(session.sessionInfo)
            .promptVersionInfo(prompt.promptInfo)
            .callInfo(callInfo)
            .responseInfo(responseInfo)
            .traceInfo(trace)
    ).await()

    trace.recordOutput(projectId, "output str").await()
    println("Recorded with completionId ${recordResponse.completionId}")
}
