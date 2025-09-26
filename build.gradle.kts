plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://oss.sonatype.org/content/repositories/snapshots/"))
            // Read from environment variables (CI) or project properties (local)
            username.set(System.getenv("OSSRH_USERNAME") ?: (project.properties["ossrhUsername"] ?: "").toString())
            password.set(System.getenv("OSSRH_PASSWORD") ?: (project.properties["ossrhPassword"] ?: "").toString())
        }
    }
}
