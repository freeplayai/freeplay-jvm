plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

nexusPublishing {
    packageGroup = "ai.freeplay"  // Explicitly set the package group
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            // Read from environment variables (CI) or project properties (local)
            username.set(System.getenv("OSSRH_USERNAME") ?: (project.properties["ossrhUsername"] ?: "").toString())
            password.set(System.getenv("OSSRH_PASSWORD") ?: (project.properties["ossrhPassword"] ?: "").toString())
        }
    }
}
