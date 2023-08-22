plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("ai.freeplay.example.java.TextCompletion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
