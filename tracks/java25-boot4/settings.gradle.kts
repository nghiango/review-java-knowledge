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

rootProject.name = "lab-track-java25-boot4"

includeBuild("build-logic")
include("test-support")
include("modules:01-core-java")
include("modules:03-concurrency")
include("modules:06-spring-mvc")
include("modules:07-spring-transactions")
include("modules:10-rest-api")
include("modules:11-spring-security")
include("modules:12-testing")
