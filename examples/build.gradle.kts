plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
