
plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.4.0")

    // Don't take a jackson(jr) dependency beyond this, as they pull in more transitive dependencies
    implementation("com.fasterxml.jackson.jr:jackson-jr-all:2.15.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
