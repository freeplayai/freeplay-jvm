plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
