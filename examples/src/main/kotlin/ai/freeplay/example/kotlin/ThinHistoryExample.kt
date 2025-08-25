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

    val questions = listOf(
            "who was the first president of the united states?",
            "what color is the sky?",
            "what shape is the earth?",
            "repeat the first question and answer"
    )
    val articles = listOf(
            "george washington was the first president of the united states",
            "the sky is blue",
            "the earth is round",
            ""
    )
    val history = mutableListOf<ChatMessage>()

    println("Getting the prompt...")
    val template = fpClient.prompts()
        .get(
            projectId,
            "History-QA",
            "latest"
        ).await()

    val sessionInfo = fpClient.sessions().create()
            .customMetadata(mapOf("custom_field" to "custom_value"))
            .sessionInfo

    for (i in 1..questions.size){
        val variables = mapOf("question" to questions[i-1], "article" to articles[i-1])
        println("variables: $variables")

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

        if (allMessages.size >= 2) {
            history.add(allMessages[allMessages.size - 2])
            history.add(allMessages[allMessages.size - 1])
        } else if (allMessages.isNotEmpty()) {
            history.addAll(allMessages)
        }

        val callInfo = CallInfo.from(
                formatted.promptInfo,
                startTime,
                System.currentTimeMillis()
        )
        val responseInfo = ResponseInfo("stop_sequence" == bodyNode.path("stop_reason").asText())

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

    }
}
