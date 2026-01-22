package ai.freeplay.example.kotlin

import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.resources.prompts.ChatMessage
import ai.freeplay.client.thin.resources.prompts.Prompts
import ai.freeplay.client.thin.resources.recordings.CallInfo
import ai.freeplay.client.thin.resources.recordings.RecordInfo
import ai.freeplay.client.thin.resources.recordings.ResponseInfo
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.*
import software.amazon.awssdk.protocols.jsoncore.JsonNode
import software.amazon.awssdk.core.document.Document

// Tool functions
fun addNumbers(numbers: List<Int>): Int = numbers.sum()

fun multipleNumbers(numbers: List<Int>): Int = numbers.fold(1) { acc, n -> acc * n }

fun subtractTwoNumbers(a: Int, b: Int): Int = a - b

fun divideTwoNumbers(a: Int, b: Int): Double = a.toDouble() / b.toDouble()

fun executeFunction(funcName: String, args: Map<String, Any>): Any {
    return when (funcName) {
        "add_numbers" -> {
            @Suppress("UNCHECKED_CAST")
            val numbers = args["numbers"] as List<Int>
            addNumbers(numbers)
        }
        "multiple_numbers" -> {
            @Suppress("UNCHECKED_CAST")
            val numbers = args["numbers"] as List<Int>
            multipleNumbers(numbers)
        }
        "subtract_two_numbers" -> {
            val a = args["a"] as Int
            val b = args["b"] as Int
            subtractTwoNumbers(a, b)
        }
        "divide_two_numbers" -> {
            val a = args["a"] as Int
            val b = args["b"] as Int
            divideTwoNumbers(a, b)
        }
        else -> throw IllegalArgumentException("Function not found: $funcName")
    }
}

// Tool specifications
val toolsSpec = listOf(
    Tool.builder()
        .toolSpec(
            ToolSpecification.builder()
                .name("add_numbers")
                .description("Add a list of numbers")
                .inputSchema(
                    ToolInputSchema.builder()
                        .json(
                            Document.fromMap(mapOf(
                                "type" to Document.fromString("object"),
                                "properties" to Document.fromMap(mapOf(
                                    "numbers" to Document.fromMap(mapOf(
                                        "type" to Document.fromString("array"),
                                        "items" to Document.fromMap(mapOf(
                                            "type" to Document.fromString("integer")
                                        )),
                                        "description" to Document.fromString("List of numbers to add")
                                    ))
                                )),
                                "required" to Document.fromList(listOf(
                                    Document.fromString("numbers")
                                ))
                            ))
                        )
                        .build()
                )
                .build()
        )
        .build(),
    Tool.builder()
        .toolSpec(
            ToolSpecification.builder()
                .name("multiple_numbers")
                .description("Multiply a list of numbers")
                .inputSchema(
                    ToolInputSchema.builder()
                        .json(
                            Document.fromMap(mapOf(
                                "type" to Document.fromString("object"),
                                "properties" to Document.fromMap(mapOf(
                                    "numbers" to Document.fromMap(mapOf(
                                        "type" to Document.fromString("array"),
                                        "items" to Document.fromMap(mapOf(
                                            "type" to Document.fromString("integer")
                                        )),
                                        "description" to Document.fromString("List of numbers to multiply")
                                    ))
                                )),
                                "required" to Document.fromList(listOf(
                                    Document.fromString("numbers")
                                ))
                            ))
                        )
                        .build()
                )
                .build()
        )
        .build(),
    Tool.builder()
        .toolSpec(
            ToolSpecification.builder()
                .name("subtract_two_numbers")
                .description("Subtract two numbers")
                .inputSchema(
                    ToolInputSchema.builder()
                        .json(
                            Document.fromMap(mapOf(
                                "type" to Document.fromString("object"),
                                "properties" to Document.fromMap(mapOf(
                                    "a" to Document.fromMap(mapOf(
                                        "type" to Document.fromString("integer"),
                                        "description" to Document.fromString("First number")
                                    )),
                                    "b" to Document.fromMap(mapOf(
                                        "type" to Document.fromString("integer"),
                                        "description" to Document.fromString("Second number")
                                    ))
                                )),
                                "required" to Document.fromList(listOf(
                                    Document.fromString("a"),
                                    Document.fromString("b")
                                ))
                            ))
                        )
                        .build()
                )
                .build()
        )
        .build(),
    Tool.builder()
        .toolSpec(
            ToolSpecification.builder()
                .name("divide_two_numbers")
                .description("Divide two numbers")
                .inputSchema(
                    ToolInputSchema.builder()
                        .json(
                            Document.fromMap(mapOf(
                                "type" to Document.fromString("object"),
                                "properties" to Document.fromMap(mapOf(
                                    "a" to Document.fromMap(mapOf(
                                        "type" to Document.fromString("integer"),
                                        "description" to Document.fromString("First number")
                                    )),
                                    "b" to Document.fromMap(mapOf(
                                        "type" to Document.fromString("integer"),
                                        "description" to Document.fromString("Second number")
                                    ))
                                )),
                                "required" to Document.fromList(listOf(
                                    Document.fromString("a"),
                                    Document.fromString("b")
                                ))
                            ))
                        )
                        .build()
                )
                .build()
        )
        .build()
)

fun main(): Unit = runBlocking {
    val objectMapper = ObjectMapper()

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
    val equation = "2x + 5 = 10"
    val promptVars = mapOf("equation" to equation)

    // Get formatted prompt
    // TODO: Update "nova_tool_call" to match your template name in Freeplay
    val formattedPrompt = freeplayClient.prompts()
        .getFormatted<List<Map<String, Any>>>(
            Prompts.GetFormattedRequest(
                projectId,
                "nova_tool_call",  // <-- Change this to your template name
                "latest",
                promptVars
            ).history(emptyList())  // Pass empty history initially
        ).await()

    println("Using model: ${formattedPrompt.promptInfo.model}")
    println("Template: ${formattedPrompt.promptInfo.templateName}")

    // Create session and trace
    val session = freeplayClient.sessions().create()
    val trace = session.createTrace(equation).agentName("math_solver")

    // Initialize history with formatted prompt messages
    val history = mutableListOf<Message>()
    formattedPrompt.formattedPrompt.forEach { msgMap ->
        @Suppress("UNCHECKED_CAST")
        val role = msgMap["role"] as? String ?: return@forEach
        val content: Any? = msgMap["content"]

        val contentBlocks = when {
            content is String -> listOf(ContentBlock.fromText(content))
            content is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                (content as List<Map<String, Any>>).map { item ->
                    when {
                        item.containsKey("text") -> ContentBlock.fromText(item["text"]?.toString() ?: "")
                        else -> throw IllegalArgumentException("Unsupported content type")
                    }
                }
            }
            else -> throw IllegalArgumentException("Unsupported content format")
        }

        history.add(
            Message.builder()
                .role(role)
                .content(contentBlocks)
                .build()
        )
    }

    // Chat messages for recording - use raw completion format for all messages
    val chatHistory = mutableListOf<ChatMessage>()
    formattedPrompt.formattedPrompt.forEach { msgMap ->
        // Add as raw completion message to preserve structure
        chatHistory.add(ChatMessage(msgMap))
    }

    var finishReason: StopReason? = null
    while (finishReason != StopReason.END_TURN && finishReason != StopReason.STOP_SEQUENCE) {
        val start = System.currentTimeMillis()

        // Make Bedrock Converse call
        val systemContent = formattedPrompt.systemContent.orElse("")
        val request = ConverseRequest.builder()
            .modelId(formattedPrompt.promptInfo.model)
            .messages(history)
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
            .toolConfig(ToolConfiguration.builder().tools(toolsSpec).build())
            .build()

        val response = converseClient.converse(request)
        val end = System.currentTimeMillis()

        val outputMessage = response.output().message()
        finishReason = response.stopReason()

        println("\nStop reason: $finishReason")

        if (finishReason == StopReason.TOOL_USE) {
            // Find the toolUse in content
            val toolUse = outputMessage.content().firstNotNullOfOrNull { it.toolUse() }
                ?: throw IllegalStateException("No toolUse found in response")

            val toolName = toolUse.name()
            val toolInputJson = toolUse.input().toString()
            val toolId = toolUse.toolUseId()

            @Suppress("UNCHECKED_CAST")
            val toolInput = objectMapper.readValue(toolInputJson, Map::class.java) as Map<String, Any>

            println("\nExecuting function $toolName with args ${objectMapper.writeValueAsString(toolInput)}")
            val result = executeFunction(toolName, toolInput)
            println("Result: $result\n")

            // Add the full assistant response to history
            println("=== Adding assistant message to history ===")
            history.add(outputMessage)

            // Add assistant message to chat history as raw Bedrock format for proper recording
            val assistantMessageMap = mapOf(
                "role" to "assistant",
                "content" to outputMessage.content().map { contentBlock ->
                    when {
                        contentBlock.text() != null -> mapOf("text" to contentBlock.text())
                        contentBlock.toolUse() != null -> {
                            val tool = contentBlock.toolUse()
                            mapOf(
                                "toolUse" to mapOf(
                                    "toolUseId" to tool.toolUseId(),
                                    "name" to tool.name(),
                                    "input" to objectMapper.readValue(tool.input().toString(), Map::class.java)
                                )
                            )
                        }
                        else -> mapOf()
                    }
                }
            )
            chatHistory.add(ChatMessage(assistantMessageMap))

            // Add the tool response to history
            val toolResultMessage = Message.builder()
                .role("user")
                .content(
                    ContentBlock.fromToolResult(
                        ToolResultBlock.builder()
                            .toolUseId(toolId)
                            .content(
                                listOf(
                                    ToolResultContentBlock.fromText(result.toString())
                                )
                            )
                            .build()
                    )
                )
                .build()

            println("\n=== Adding tool result to history ===")
            history.add(toolResultMessage)

            // Add tool result to chat history as raw Bedrock format
            val toolResultMessageMap = mapOf(
                "role" to "user",
                "content" to listOf(
                    mapOf(
                        "toolResult" to mapOf(
                            "toolUseId" to toolId,
                            "content" to listOf(
                                mapOf("text" to result.toString())
                            )
                        )
                    )
                )
            )
            chatHistory.add(ChatMessage(toolResultMessageMap))

            // Record the tool call to Freeplay
            println("\n=== Recording to Freeplay ===")
            println("Chat history length: ${chatHistory.size}")

            freeplayClient.recordings().create(
                RecordInfo(
                    projectId,
                    chatHistory.toList()
                ).inputs(promptVars)
                    .sessionInfo(session.sessionInfo)
                    .promptVersionInfo(formattedPrompt.promptInfo)
                    .callInfo(CallInfo.from(formattedPrompt.promptInfo, start, end))
                    .responseInfo(ResponseInfo(false))
                    .parentId(trace.traceId)
            ).await()

            println("\n✓ Successfully recorded to Freeplay")
        } else {
            // Final response
            val content = outputMessage.content().first().text()
            println("=== Solution ===")
            println(content)
            println("\n")

            // Add the final response to history
            println("=== Adding final response to history ===")
            history.add(outputMessage)

            // Add final response to chat history as raw Bedrock format
            val finalMessageMap = mapOf(
                "role" to "assistant",
                "content" to outputMessage.content().map { contentBlock ->
                    when {
                        contentBlock.text() != null -> mapOf("text" to contentBlock.text())
                        else -> mapOf()
                    }
                }
            )
            chatHistory.add(ChatMessage(finalMessageMap))

            // Record the final response to Freeplay
            println("\n=== Recording final response to Freeplay ===")
            println("Chat history length: ${chatHistory.size}")

            freeplayClient.recordings().create(
                RecordInfo(
                    projectId,
                    chatHistory.toList()
                ).inputs(promptVars)
                    .sessionInfo(session.sessionInfo)
                    .promptVersionInfo(formattedPrompt.promptInfo)
                    .callInfo(CallInfo.from(formattedPrompt.promptInfo, start, end))
                    .responseInfo(ResponseInfo(true))
                    .parentId(trace.traceId)
            ).await()

            println("\n✓ Successfully recorded to Freeplay")

            trace.recordOutput(projectId, content).await()
        }
    }
}
