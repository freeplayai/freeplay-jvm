plugins {
    application
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("software.amazon.awssdk:sagemakerruntime:2.25.50")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
