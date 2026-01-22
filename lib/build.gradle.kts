import java.net.HttpURLConnection
import java.net.URL
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
}

version = "0.4.5"
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
    testImplementation("software.amazon.awssdk:bedrockruntime:2.35.3")

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

tasks.register("printClasspath") {
    group = "help"
    description = "Prints the runtime classpath for use with jshell REPL"
    doLast {
        println(sourceSets["main"].runtimeClasspath.asPath)
    }
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
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
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
                    connection.set("scm:git:git://github.com/freeplayai/freeplay-jvm.git")
                    developerConnection.set("scm:git:ssh://github.com/freeplayai/freeplay-jvm.git")
                    url.set("https://github.com/freeplayai/freeplay-jvm")
                }
            }
        }
    }

    // Repository configuration is handled automatically by the nexus-publish plugin
}


signing {
    // Configure signing using environment variables (for CI) or properties (for local)
    val signingKeyId = System.getenv("SIGNING_KEY_ID") ?: project.findProperty("signing.keyId")?.toString()
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signing.password")?.toString()  
    val signingKeyRingFile = System.getenv("SIGNING_SECRET_KEY_RING_FILE") ?: project.findProperty("signing.secretKeyRingFile")?.toString()
    
    if (signingKeyId != null && signingPassword != null && signingKeyRingFile != null) {
        project.ext["signing.keyId"] = signingKeyId
        project.ext["signing.password"] = signingPassword
        project.ext["signing.secretKeyRingFile"] = signingKeyRingFile
        
        sign(publishing.publications["mavenJava"])
    }
}



tasks.register("determineVersion") {
    group = "publishing"
    description = "Determine the next version based on release type"
    
    doLast {
        val releaseType = project.properties["releaseType"]?.toString() ?: "prerelease"
        val currentVersion = project.version.toString()
        
        // Parse current version (e.g., "0.4.3-alpha1" -> base: "0.4.3", suffix: "alpha1")
        val versionRegex = """^(\d+\.\d+\.\d+)(?:-(.+))?$""".toRegex()
        val matchResult = versionRegex.find(currentVersion)
            ?: throw GradleException("Invalid version format: $currentVersion")
        
        val baseVersion = matchResult.groupValues[1]
        
        when (releaseType) {
            "stable" -> {
                // For stable releases, check if the EXACT current version already exists
                // User must manually bump version in build.gradle.kts (e.g., 0.4.3-alpha1 -> 0.4.4)
                if (checkVersionExistsOnSonatype(currentVersion)) {
                    throw GradleException("Version $currentVersion already exists on Sonatype. Please manually bump the version in build.gradle.kts")
                }
                
                println("RESULT_JSON:{\"current_version\":\"$currentVersion\",\"base_version\":\"$baseVersion\",\"new_version\":\"$currentVersion\",\"is_prerelease\":false}")
            }
            
            "prerelease" -> {
                // Find the latest alpha version on Sonatype
                val latestAlpha = findLatestAlphaVersion(baseVersion)
                val nextAlphaNumber = if (latestAlpha != null) {
                    val alphaRegex = """alpha(\d+)""".toRegex()
                    val alphaMatch = alphaRegex.find(latestAlpha)
                    if (alphaMatch != null) {
                        alphaMatch.groupValues[1].toInt() + 1
                    } else {
                        1
                    }
                } else {
                    1
                }
                
                val newVersion = "$baseVersion-alpha$nextAlphaNumber"
                
                println("RESULT_JSON:{\"current_version\":\"$currentVersion\",\"base_version\":\"$baseVersion\",\"new_version\":\"$newVersion\",\"is_prerelease\":true}")
            }
            
            else -> throw GradleException("Invalid release type: $releaseType. Must be 'stable' or 'prerelease'")
        }
    }
}

fun checkVersionExistsOnSonatype(version: String): Boolean {
    return try {
        val url = URL("https://repo1.maven.org/maven2/ai/freeplay/client/$version/")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.responseCode == 200
    } catch (e: Exception) {
        false
    }
}

fun findLatestAlphaVersion(baseVersion: String): String? {
    return try {
        val url = URL("https://repo1.maven.org/maven2/ai/freeplay/client/maven-metadata.xml")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val alphaVersions = mutableListOf<String>()
            
            // Simple XML parsing to extract versions
            val versionRegex = """<version>($baseVersion-alpha\d+)</version>""".toRegex()
            versionRegex.findAll(response).forEach { match ->
                alphaVersions.add(match.groupValues[1])
            }
            
            // Sort and return the latest
            alphaVersions.sortedWith { a, b ->
                val aNum = """alpha(\d+)""".toRegex().find(a)?.groupValues?.get(1)?.toInt() ?: 0
                val bNum = """alpha(\d+)""".toRegex().find(b)?.groupValues?.get(1)?.toInt() ?: 0
                aNum.compareTo(bNum)
            }.lastOrNull()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}


tasks.register("publishAll") {
    dependsOn("clean")
    dependsOn("test")
    dependsOn("jar")
    dependsOn("javadocJar")
    dependsOn("sourcesJar")
    dependsOn("publishMavenJavaPublicationToSonatypeRepository")
}
