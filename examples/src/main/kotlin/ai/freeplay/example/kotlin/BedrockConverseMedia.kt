package ai.freeplay.example.kotlin

import ai.freeplay.client.media.MediaInputBase64
import ai.freeplay.client.media.MediaInputCollection
import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.prompts.Prompts
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.*
import java.net.URL
import java.util.Base64

// Helper function to load image from URL with proper headers
data class ImageData(val bytes: ByteArray, val format: String, val contentType: String)

fun loadImageFromUrl(imageUrl: String): ImageData {
    val url = URL(imageUrl)
    val connection = url.openConnection()

    // Add headers to avoid 403 errors
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
    connection.setRequestProperty("Accept", "image/*")

    val imageBytes = connection.getInputStream().readBytes()

    val contentType = connection.contentType ?: ""

    // Determine format from content-type or URL
    val imageFormat = when {
        contentType.contains("jpeg", ignoreCase = true) || contentType.contains("jpg", ignoreCase = true) -> "jpeg"
        contentType.contains("png", ignoreCase = true) -> "png"
        contentType.contains("gif", ignoreCase = true) -> "gif"
        contentType.contains("webp", ignoreCase = true) -> "webp"
        else -> {
            // Try to infer from URL
            val ext = imageUrl.lowercase().split(".").last().split("?").first()
            val formatMap = mapOf(
                "jpg" to "jpeg",
                "jpeg" to "jpeg",
                "png" to "png",
                "gif" to "gif",
                "webp" to "webp"
            )
            formatMap[ext] ?: "jpeg"
        }
    }

    return ImageData(imageBytes, imageFormat, "image/$imageFormat")
}

fun main(): Unit = runBlocking {
    // Initialize Freeplay client
    val freeplayClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(System.getenv("FREEPLAY_API_KEY"))
            .baseUrl("${System.getenv("FREEPLAY_API_URL")}/api")
    )

    // Initialize Bedrock client
    val converseClient = BedrockRuntimeClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    System.getenv("AWS_ACCESS_KEY_ID"),
                    System.getenv("AWS_SECRET_ACCESS_KEY")
                )
            )
        )
        .build()

    val projectId = System.getenv("FREEPLAY_PROJECT_ID")

    val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/1200px-Cat03.jpg"

    // Download the image
    println("Downloading image from: $imageUrl")
    val imageData = loadImageFromUrl(imageUrl)
    println("Downloaded image (format: ${imageData.format}, size: ${imageData.bytes.size} bytes)")

    // Question about the image
    val question = "What do you see in this image? Describe it in detail."
    val promptVars = mapOf("question" to question)

    val mediaInputs = MediaInputCollection()
    // MediaInputBase64 expects base64-encoded bytes, not raw bytes
    val base64EncodedBytes = Base64.getEncoder().encode(imageData.bytes)
    mediaInputs.put("city-image", MediaInputBase64(base64EncodedBytes, imageData.contentType))

    // Get formatted prompt from Freeplay
    // TODO: Update "nova_image_test" to match your template name in Freeplay
    val formattedPrompt = freeplayClient.prompts()
        .getFormatted<List<Map<String, Any>>>(
            Prompts.GetFormattedRequest(
                projectId,
                "nova_image_test",  // <-- Change this to your template name
                "latest",
                promptVars
            ).mediaInputs(mediaInputs)
        ).await()

    // Construct messages for Bedrock API (with raw bytes)
    val bedrockMessages = listOf(
        Message.builder()
            .role("user")
            .content(
                listOf(
                    ContentBlock.fromImage(
                        ImageBlock.builder()
                            .format(imageData.format)
                            .source(
                                ImageSource.builder()
                                    .bytes(SdkBytes.fromByteArray(imageData.bytes))
                                    .build()
                            )
                            .build()
                    ),
                    ContentBlock.fromText(question)
                )
            )
            .build()
    )

    val start = System.currentTimeMillis()

    // Create session
    val session = freeplayClient.sessions().create()

    // Call Bedrock API
    val systemContent = formattedPrompt.systemContent.orElse("")
    val request = ConverseRequest.builder()
        .modelId(formattedPrompt.promptInfo.model)
        .messages(bedrockMessages)
        .apply {
            if (systemContent.isNotEmpty()) {
                system(listOf(SystemContentBlock.fromText(systemContent)))
            }
        }
        .inferenceConfig(
            InferenceConfiguration.builder()
                .also { builder ->
                    formattedPrompt.promptInfo.modelParameters?.let { params ->
                        (params["temperature"] as? Number)?.toFloat()?.let { builder.temperature(it) }
                        (params["maxTokens"] as? Number)?.toInt()?.let { builder.maxTokens(it) }
                        (params["topP"] as? Number)?.toFloat()?.let { builder.topP(it) }
                    }
                }
                .build()
        )
        .build()

    val response = converseClient.converse(request)
    val end = System.currentTimeMillis()

    val outputMessage = response.output().message()
    val responseContent = outputMessage.content().first().text()

    println("\nUsing model: ${formattedPrompt.promptInfo.model}")
    println("Template: ${formattedPrompt.promptInfo.templateName}")
    println("\n=== Model Response ===")
    println(responseContent)

    println("\n=== Recording to Freeplay ===")

    // Record using ChatMessage format
    val allMessages = listOf(
        ChatMessage("user", question),
        ChatMessage("assistant", responseContent)
    )

    freeplayClient.recordings().create(
        RecordInfo(
            projectId,
            allMessages
        ).inputs(promptVars)
            .sessionInfo(session.sessionInfo)
            .promptVersionInfo(formattedPrompt.promptInfo)
            .callInfo(CallInfo.from(formattedPrompt.promptInfo, start, end))
            .responseInfo(ResponseInfo(true))
            .mediaInputCollection(mediaInputs)
    ).await()

    println("Successfully recorded to Freeplay")
}
