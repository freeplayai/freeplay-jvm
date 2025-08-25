package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.generativeai.GenerativeModel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking


fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"
    val googleProjectId = System.getenv("EXAMPLES_VERTEX_PROJECT_ID")

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    )

    val variables = mapOf("question" to "Why isn't my window working?")

    println("Getting the prompt...")
    val prompt = fpClient.prompts()
        .getFormatted<List<Content>>(
            projectId,
            "my-gemini-prompt",
            "latest",
            variables
        ).await()

    val startTime = System.currentTimeMillis()
    val completion = VertexAI(googleProjectId, "us-central1").use { vertexAi ->
        val model = GenerativeModel(prompt.promptInfo.model, vertexAi)
        val response = model.generateContent(prompt.formattedPrompt)
        return@use response.getCandidates(0).content.getParts(0).text
    }

    println("Completion: $completion")

    println("Recording the result")
    val allMessages: List<ChatMessage> = prompt.allMessages(
        ChatMessage("assistant", completion)
    )
    val callInfo = CallInfo.from(
        prompt.promptInfo,
        startTime,
        System.currentTimeMillis()
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