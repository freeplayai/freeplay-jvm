package ai.freeplay.example.kotlin

import ai.freeplay.client.Freeplay
import ai.freeplay.client.resources.agentdatasets.AgentTestCaseInput
import ai.freeplay.client.resources.agentdatasets.CreateAgentDatasetRequest
import ai.freeplay.client.resources.agentdatasets.UpdateAgentDatasetRequest
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val freeplayApiKey = System.getenv("FREEPLAY_API_KEY")
    val projectId = System.getenv("FREEPLAY_PROJECT_ID")
    val baseUrl = System.getenv("FREEPLAY_API_URL") + "/api"

    val fpClient = Freeplay(
        Freeplay.Config()
            .freeplayAPIKey(freeplayApiKey)
            .baseUrl(baseUrl)
    )

    println("Creating agent dataset...")
    val dataset = fpClient.agentDatasets().create(
        projectId,
        CreateAgentDatasetRequest("Travel Agent Test Cases")
            .description("End-to-end test cases for the travel booking agent")
            // compatibleAgentIds takes a list of agent UUIDs from your Freeplay project
            // .compatibleAgentIds(listOf("<your-agent-uuid>"))
    ).await()
    println("Created dataset: ${dataset.id} - ${dataset.name}")

    println("\nListing agent datasets...")
    val datasets = fpClient.agentDatasets().list(projectId).await()
    println("Found ${datasets.data.size} dataset(s)")

    println("\nFetching dataset ${dataset.id}...")
    val fetchedDataset = fpClient.agentDatasets().get(projectId, dataset.id).await()
    println("Fetched: ${fetchedDataset.name}, compatibleAgentIds=${fetchedDataset.compatibleAgentIds}")

    println("\nAdding test cases...")
    val testCases = fpClient.agentDatasets().createTestCases(
        projectId,
        dataset.id,
        listOf(
            AgentTestCaseInput()
                .inputs(mapOf("request" to "Book a flight from NYC to LAX on Friday"))
                .outputs(mapOf("booked" to true, "confirmation" to "ABC123"))
                .metadata(mapOf("scenario" to "happy-path")),
            AgentTestCaseInput()
                .inputs(mapOf("request" to "Cancel my reservation"))
                .outputs(mapOf("cancelled" to true))
        )
    ).await()
    println("Created ${testCases.size} test case(s)")

    println("\nListing test cases...")
    val testCaseList = fpClient.agentDatasets().listTestCases(projectId, dataset.id).await()
    for (tc in testCaseList.data) {
        println("  [${tc.id}] input=${tc.input} output=${tc.output}")
    }

    println("\nUpdating dataset...")
    val updated = fpClient.agentDatasets().update(
        projectId,
        dataset.id,
        UpdateAgentDatasetRequest().description("Updated end-to-end test cases")
    ).await()
    println("Updated dataset description: ${updated.description}")

//    // Bulk delete all test cases
//    if (testCaseList.data.isNotEmpty()) {
//        val ids = testCaseList.data.map { it.id }
//        println("\nBulk deleting ${ids.size} test case(s)...")
//        fpClient.agentDatasets().deleteTestCases(projectId, dataset.id, ids).await()
//        println("Deleted.")
//    }
//
//    // Delete the dataset
//    println("\nDeleting dataset ${dataset.id}...")
//    fpClient.agentDatasets().delete(projectId, dataset.id).await()
//    println("Dataset deleted.")
}
