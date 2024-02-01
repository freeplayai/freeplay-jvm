package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
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
    val customerDomain = System.getenv("FREEPLAY_CUSTOMER_NAME")
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .customerDomain(customerDomain)
    )

    val variables = mapOf("question" to "Why isn't my window working?")

    println("Getting the prompt...")
    val prompt = fpClient.prompts()
        .getFormatted<String>(
            projectId,
            "my-prompt-anthropic",
            "prod",
            variables
        ).await()

    println("Calling Anthropic...")
    val startTime = System.currentTimeMillis()
    val llmResponse = callAnthropic(
        objectMapper,
        anthropicApiKey,
        prompt.promptInfo.model,
        prompt.promptInfo.modelParameters,
        prompt.formattedPrompt
    ).await()

    val bodyNode = objectMapper.readTree(llmResponse.body())
    println("Completion: " + bodyNode.path("completion").asText())

    println("Recording the result")
    val allMessages: List<ChatMessage> = prompt.allMessages(
        ChatMessage("Assistant", bodyNode.path("completion").asText())
    )
    val callInfo = CallInfo.from(
        prompt.getPromptInfo(),
        startTime,
        System.currentTimeMillis()
    )
    val responseInfo = ResponseInfo("stop_sequence" == bodyNode.path("stop_reason").asText())
    val sessionInfo = fpClient.sessions().create()
        .customMetadata(mapOf("custom_field" to "custom_value"))
        .sessionInfo

    val recordResponse = fpClient.recordings().create(
        RecordInfo(
            allMessages,
            variables,
            sessionInfo,
            prompt.getPromptInfo(),
            callInfo,
            responseInfo
        )
    ).await()
    println("Recorded with completionId ${recordResponse.completionId}")
}
