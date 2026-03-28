plugins {
    application
    kotlin("jvm") version "1.9.22"
}

application {
    mainClass.set(project.findProperty("mainClass") as String? ?: "ai.freeplay.example.QuickMetadataTest")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))

    // If a customer wants to only use our formatting and won't use the Vertex SDK
    // themselves, they can just include this line.
    runtimeOnly(project(":lib")) {
        capabilities {
            requireCapability("ai.freeplay:lib-gemini")
        }
    }

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("software.amazon.awssdk:sagemakerruntime:2.25.50")
    implementation("software.amazon.awssdk:bedrockruntime:2.35.3")
    // If a customer is using the Vertex SDK themselves, they include this line.
    // If they have this line, they don't need the runtimeOnly declaration above.
    implementation("com.google.cloud:google-cloud-vertexai:1.5.0")

    constraints {
        implementation("io.netty:netty-codec-http:4.1.132.Final")
        implementation("io.netty:netty-codec-http2:4.1.132.Final")
        implementation("io.grpc:grpc-netty-shaded:1.75.0")
        implementation("com.google.protobuf:protobuf-java:3.25.5")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
