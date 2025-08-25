package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import ai.freeplay.example.java.ThinExampleUtils.callBaseten
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

private val objectMapper = ObjectMapper()

fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"
    val basetenApiKey = System.getenv("BASETEN_API_KEY")
    val modelId = System.getenv("BASETEN_MODEL_ID")

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    )

    val variables = mapOf("question" to "Why isn't my window working?")

    println("Getting the prompt...")
    val prompt = fpClient.prompts()
        .getFormatted<List<ChatMessage>>(
            projectId,
            "my-baseten-mistral-prompt",
            "latest",
            variables
        ).await()

    val startTime = System.currentTimeMillis()
    val response = callBaseten(
        objectMapper,
        basetenApiKey,
        modelId,
        prompt.promptInfo.modelParameters,
        prompt.formattedPrompt
    ).await()
    val endTime = System.currentTimeMillis()

    val body = response.body()
    println(body)

    println("Recording the result")
    val allMessages: List<ChatMessage> = prompt.allMessages(
        ChatMessage("assistant", body)
    )
    val callInfo = CallInfo.from(
        prompt.promptInfo,
        startTime,
        endTime
    )
    val responseInfo = ResponseInfo(true)
    val sessionInfo = fpClient.sessions().create()
        .customMetadata(mapOf("custom_field" to "custom_value"))
        .sessionInfo

    val recordResponse = fpClient.recordings().create(
        RecordInfo(
            projectId,
            allMessages
        ).inputs(variables)
            .sessionInfo(sessionInfo)
            .promptVersionInfo(prompt.promptInfo)
            .callInfo(callInfo)
            .responseInfo(responseInfo)
    ).await()
    println("Recorded with completionId ${recordResponse.completionId}")
}