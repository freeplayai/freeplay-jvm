package ai.freeplay.example.kotlin

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

    val templatePrompt = fpClient.prompts().get(projectId, "my-anthropic-prompt", "prod").await()
    val testRun = fpClient.testRuns().create(projectId, "core-tests").await()

    for (testCase in testRun.testCases) {
        val formattedPrompt = templatePrompt.bind(TemplatePrompt.BindRequest(testCase.variables)).format<List<ChatMessage>>()

        val startTime = System.currentTimeMillis()
        val llmResponse = callAnthropic(
            objectMapper,
            anthropicApiKey,
            formattedPrompt.promptInfo.model,
            formattedPrompt.promptInfo.modelParameters,
            formattedPrompt.formattedPrompt,
            formattedPrompt.systemContent.orElse(null)
        ).await()

        val bodyNode = objectMapper.readTree(llmResponse.body())

        println("Recording the result")
        val allMessages = formattedPrompt.allMessages(
            ChatMessage("assistant", bodyNode.path("content").get(0).get("text").asText())
        )
        val callInfo = CallInfo.from(
            formattedPrompt.getPromptInfo(),
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
            ).inputs(testCase.variables)
                .sessionInfo(sessionInfo)
                .promptVersionInfo(formattedPrompt.getPromptInfo())
                .callInfo(callInfo)
                .responseInfo(responseInfo)
                .testRunInfo(testRun.getTestRunInfo(testCase.testCaseId))
        ).await()
        println("Recorded with completionId ${recordResponse.completionId}")
    }
}
