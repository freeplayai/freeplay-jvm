package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient
import software.amazon.awssdk.services.sagemakerruntime.model.InvokeEndpointRequest

private val objectMapper = ObjectMapper()

fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    )

    val variables = mapOf("question" to "Why isn't my window working?")

    println("Getting the prompt...")
    val prompt = fpClient.prompts()
        .getFormatted<String>(
            projectId,
            "my-sagemaker-llama-3-prompt",
            "latest",
            variables
        ).await()

    val endpointName = prompt.promptInfo.providerInfo["endpoint_name"] as String
    val inferenceComponent = prompt.promptInfo.providerInfo["inference_component_name"] as String

    val client = SageMakerRuntimeClient.builder().region(Region.US_EAST_1).build()

    val llMParameters = mapOf(
        "inputs" to prompt.formattedPrompt.toString(),
        "parameters" to prompt.promptInfo.modelParameters
    )
    val requestBody = objectMapper.writeValueAsString(llMParameters)

    val startTime = System.currentTimeMillis()
    val response = client.invokeEndpoint(
        InvokeEndpointRequest.builder()
            .accept("application/json")
            .contentType("application/json")
            .endpointName(endpointName)
            .inferenceComponentName(inferenceComponent)
            .body(SdkBytes.fromUtf8String(requestBody))
            .build()
    )

    val body = response.body().asUtf8String()
    val completion = objectMapper.readTree(body)["generated_text"].asText()
    println(completion)

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