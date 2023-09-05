plugins {
    `java-library`
    `maven-publish`
    signing
}

version = "0.0.1"
group = "ai.freeplay"

repositories {
    //  uncomment the next line for local testing
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.4.0")

    // Don't take a jackson(jr) dependency beyond this, as they pull in more transitive dependencies
    implementation("com.fasterxml.jackson.jr:jackson-jr-all:2.15.2")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().resources)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(sourceSets.main.get().resources)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
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
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
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

tasks.register("publishAll") {
    dependsOn("clean")
    dependsOn("jar")
    dependsOn("javadocJar")
    dependsOn("sourcesJar")
    dependsOn("publishMavenJavaPublicationToOSSRHRepository")
}
