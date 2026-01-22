package ai.freeplay.example.kotlin

import ai.freeplay.client.model.Provider
import ai.freeplay.client.thin.Freeplay
import ai.freeplay.client.thin.internal.v2dto.TemplateDTO
import ai.freeplay.client.thin.resources.prompts.CreateVersionRequest
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"
    val promptTemplateName = System.getenv("PROMPT_TEMPLATE_NAME")

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    )

    val templateMessages = listOf(
        TemplateDTO.Message("system", "You are a helpful assistant."),
        TemplateDTO.Message("user", "{{question}}")
    )

    val llmParameters = mapOf(
        "temperature" to 0.7,
        "max_tokens" to 1000
    )

    val toolSchema = listOf(
        TemplateDTO.ToolSchema(
            "get_weather",
            "Get the current weather for a location",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "location" to mapOf(
                        "type" to "string",
                        "description" to "The city and state, e.g. San Francisco, CA"
                    )
                ),
                "required" to listOf("location")
            )
        )
    )

    val environments = listOf<String>()

    println("Creating a new prompt template version...")
    val response = fpClient.prompts().createVersion(
        CreateVersionRequest.Builder(
            projectId,
            promptTemplateName,
            templateMessages,
            "claude-3-5-sonnet-20241022",
            Provider.Anthropic
        )
            .versionName("v1.1")
            .versionDescription("Initial version of the prompt template")
            .llmParameters(llmParameters)
            .toolSchema(toolSchema)
            .environments(environments)
            .build()
    ).await()

    println("Created prompt template version:")
    println("  Template ID: ${response.promptTemplateId}")
    println("  Version ID: ${response.promptTemplateVersionId}")
    println("  Template Name: ${response.promptTemplateName}")
    println("  Version Name: ${response.versionName}")
    println("  Version Description: ${response.versionDescription}")
    println("  Provider: ${response.metadata.provider}")
    println("  Model: ${response.metadata.model}")

    println("Updating version environments...")
    val newEnvironments = listOf("dev", "prod")
    fpClient.prompts().updateVersionEnvironments(
        projectId,
        response.promptTemplateId,
        response.promptTemplateVersionId,
        newEnvironments
    ).await()

    println("Successfully updated version environments to: ${newEnvironments.joinToString(", ")}")
}
