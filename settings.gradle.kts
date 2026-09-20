pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "senior-java-spring-interview-lab"

includeBuild("build-logic")
include("modules:test-support")
include("modules:01-core-java")
include("modules:02-jvm")
include("modules:03-concurrency")
include("modules:04-spring-core")
include("modules:05-spring-boot")
include("modules:06-spring-mvc")
include("modules:07-spring-transactions")
include("modules:08-jpa-hibernate")
include("modules:09-database-sql")
include("modules:10-rest-api")
include("modules:11-spring-security")
include("modules:12-testing")
include("modules:13-caching-redis")
include("modules:14-kafka")
