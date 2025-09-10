import java.net.HttpURLConnection
import java.net.URL
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
}

version = "0.4.3"
group = "ai.freeplay"

repositories {
    //  uncomment the next line for local testing
    mavenLocal()
    mavenCentral()
}

java {
    registerFeature("gemini") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.4.0")

    implementation("com.fasterxml.jackson.jr:jackson-jr-all:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.github.spullara.mustache.java:compiler:0.9.11")
    "geminiImplementation"("com.google.cloud:google-cloud-vertexai:1.5.0")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().resources)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(sourceSets.main.get().resources)
}

tasks.jar {
    // Set the manifest attributes
    manifest {
        attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
        )
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    description = "Runs the fast tests."
    useJUnit {
        excludeCategories("ai.freeplay.client.SlowTest")
    }
}

tasks.register<Test>("slowTest") {
    group = "verification"
    description = "Runs the slow tests."
    useJUnit {
        includeCategories("ai.freeplay.client.SlowTest")
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs the entire test suite."
    dependsOn("test", "slowTest")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "client"
            from(components["java"])

            pom {
                name.set("Freeplay Java SDK")
                description.set("Freeplay Java SDK used to interact with the Freeplay API")
                url.set("https://freeplay.ai")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("freeplay")
                        name.set("Freeplay")
                        email.set("engineering@freeplay.ai")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/228Labs/freeplay-jvm.git")
                    developerConnection.set("scm:git:ssh://github.com/228Labs/freeplay-jvm.git")
                    url.set("https://github.com/228Labs/freeplay-jvm")
                }
            }
        }
    }

    repositories {
        maven {
            name = "ossrh-staging-api"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = (project.properties["ossrhUsername"] ?: "").toString()
                password = (project.properties["ossrhPassword"] ?: "").toString()
            }
        }
    }


}

signing {
    sign(publishing.publications["mavenJava"])
}



tasks.register("uploadToPortal") {
    group = "publishing"
    description = "Upload deployment to Central Publisher Portal"

    doLast {
        val username = (project.properties["ossrhUsername"] ?: "").toString()
        val password = (project.properties["ossrhPassword"] ?: "").toString()
        val namespace = "ai.freeplay" // Your namespace

        if (username.isNotEmpty() && password.isNotEmpty()) {
            val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

            val url = URL("https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $credentials")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Optional: specify publishing type (user_managed, automatic, or portal_api)
            val requestBody = """{"publishing_type": "user_managed"}"""
            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == 200 || responseCode == 201) {
                println("Successfully uploaded to Portal")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw GradleException("Failed to upload to Portal: $responseCode - $errorResponse")
            }
        } else {
            throw GradleException("Missing Portal credentials")
        }
    }
}

tasks.register("publishAll") {
    dependsOn("clean")
    dependsOn("test")
    dependsOn("jar")
    dependsOn("javadocJar")
    dependsOn("sourcesJar")
    dependsOn("publishMavenJavaPublicationToOssrh-staging-apiRepository")
    dependsOn("uploadToPortal")

    // Ensure upload happens after publish
    tasks.findByName("uploadToPortal")?.mustRunAfter("publishMavenJavaPublicationToOssrh-staging-apiRepository")
}
