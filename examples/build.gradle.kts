plugins {
    application
    kotlin("jvm") version "2.3.20"
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

    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("software.amazon.awssdk:sagemakerruntime:2.42.27")
    implementation("software.amazon.awssdk:bedrockruntime:2.42.27")
    // If a customer is using the Vertex SDK themselves, they include this line.
    // If they have this line, they don't need the runtimeOnly declaration above.
    implementation("com.google.cloud:google-cloud-vertexai:1.50.0")

    constraints {
        implementation("io.netty:netty-codec-http:4.2.12.Final")
        implementation("io.netty:netty-codec-http2:4.2.12.Final")
        implementation("io.grpc:grpc-netty-shaded:1.80.0")
        implementation("com.google.protobuf:protobuf-java:4.34.1")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
