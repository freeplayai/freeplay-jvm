package ai.freeplay.example.kotlin

import ai.freeplay.client.Freeplay
import ai.freeplay.client.resources.promptdatasets.CreatePromptDatasetRequest
import ai.freeplay.client.resources.promptdatasets.PromptTestCaseInput
import ai.freeplay.client.resources.promptdatasets.UpdatePromptDatasetRequest
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

    println("Creating prompt dataset...")
    val dataset = fpClient.promptDatasets().create(
        projectId,
        CreatePromptDatasetRequest("Customer Support QA")
            .description("Q&A pairs for customer support evaluation")
            .inputNames(listOf("question"))
            .supportHistory(false)
    ).await()
    println("Created dataset: ${dataset.id} - ${dataset.name}")

    println("\nListing prompt datasets...")
    val datasets = fpClient.promptDatasets().list(projectId).await()
    println("Found ${datasets.data.size} dataset(s)")

    println("\nFetching dataset ${dataset.id}...")
    val fetchedDataset = fpClient.promptDatasets().get(projectId, dataset.id).await()
    println("Fetched: ${fetchedDataset.name}, inputNames=${fetchedDataset.inputNames}")

    println("\nAdding test cases...")
    val testCases = fpClient.promptDatasets().createTestCases(
        projectId,
        dataset.id,
        listOf(
            PromptTestCaseInput(mapOf("question" to "How do I reset my password?"))
                .output("Visit account settings and click 'Forgot Password'."),
            PromptTestCaseInput(mapOf("question" to "Where is my order?"))
                .output("Check your order status in the Orders section.")
                .metadata(mapOf("category" to "shipping"))
        )
    ).await()
    println("Created ${testCases.size} test case(s)")

    println("\nListing test cases...")
    val testCaseList = fpClient.promptDatasets().listTestCases(projectId, dataset.id).await()
    for (tc in testCaseList.data) {
        println("  [${tc.id}] Q: ${tc.inputs["question"]} -> A: ${tc.output}")
    }

    println("\nUpdating dataset...")
    val updated = fpClient.promptDatasets().update(
        projectId,
        dataset.id,
        UpdatePromptDatasetRequest().description("Updated description")
    ).await()
    println("Updated dataset description: ${updated.description}")

// Uncomment to delete the dataset with its test cases
//
//    if (testCaseList.data.isNotEmpty()) {
//        val firstId = testCaseList.data[0].id
//        println("\nDeleting test case $firstId...")
//        fpClient.promptDatasets().deleteTestCase(projectId, dataset.id, firstId).await()
//        println("Deleted.")
//    }
//
//    println("\nDeleting dataset ${dataset.id}...")
//    fpClient.promptDatasets().delete(projectId, dataset.id).await()
//    println("Dataset deleted.")
}
