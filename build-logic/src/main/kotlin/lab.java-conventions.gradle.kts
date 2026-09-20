import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    java
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val sourceSets = extensions.getByType<SourceSetContainer>()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    add("testImplementation", libs.findLibrary("junit-jupiter").get())
    add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
    add("testImplementation", libs.findLibrary("assertj-core").get())
    add("errorprone", libs.findLibrary("errorprone-core").get())
}

val integrationTest = sourceSets.create("integrationTest") {
    java.srcDir("src/integrationTest/java")
    resources.srcDir("src/integrationTest/resources")
    compileClasspath += sourceSets.named("main").get().output
    runtimeClasspath += output + compileClasspath
}

configurations.named(integrationTest.implementationConfigurationName) {
    extendsFrom(configurations.named("testImplementation").get())
}
configurations.named(integrationTest.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.named("testRuntimeOnly").get())
}

val brokenExamples = sourceSets.create("brokenExamples") {
    java.srcDir("broken-examples")
    compileClasspath += sourceSets.named("main").get().output
}
configurations.named(brokenExamples.implementationConfigurationName) {
    extendsFrom(configurations.named("implementation").get())
}
configurations.named(brokenExamples.compileOnlyConfigurationName) {
    extendsFrom(configurations.named("compileOnly").get())
}

val examples = sourceSets.create("examples") {
    java.srcDir("src/examples/java")
    resources.srcDir("src/examples/resources")
    compileClasspath += sourceSets.named("main").get().output
    runtimeClasspath += output + compileClasspath
}
configurations.named(examples.implementationConfigurationName) {
    extendsFrom(configurations.named("implementation").get())
}

// Gradle applies `-D` to the daemon JVM, not to the forked test worker, so a documented triage
// command such as `./gradlew test -Djunit.jupiter.testmethod.order.default=...` is otherwise a
// silent no-op. Forward the JUnit keys the flake-triage docs rely on to the test process whenever
// the caller sets them, mirroring the `api.version` system property on `integrationTest`.
val junitForwardedProperties =
        listOf(
                "junit.jupiter.testmethod.order.default",
                "junit.jupiter.execution.parallel.enabled",
        )

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    junitForwardedProperties.forEach { key ->
        System.getProperty(key)?.let { value -> systemProperty(key, value) }
    }
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs Docker-backed and full-context integration tests."
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    // Testcontainers pins docker-java's Docker API version to 1.32 unless the user chooses one, and
    // Docker Engine >= 29 rejects anything below API 1.40 with HTTP 400. Pin the lowest version
    // Docker 19.03+ still serves so container tests run against new and old daemons alike;
    // Testcontainers keeps a user-supplied `api.version` instead of overriding it.
    systemProperty("api.version", "1.40")
}

tasks.register("compileBrokenExamples") {
    group = "verification"
    description = "Compiles intentionally flawed code without running it or adding it to build."
    dependsOn(tasks.named(brokenExamples.compileJavaTaskName))
}

tasks.register("compileExamples") {
    group = "verification"
    description = "Compiles question and demo example code without packaging or running it."
    dependsOn(tasks.named(examples.compileJavaTaskName))
}

tasks.named("check") {
    dependsOn("compileExamples")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
    if (name == brokenExamples.compileJavaTaskName) {
        options.errorprone.enabled.set(false)
    }
}

extensions.configure<SpotlessExtension> {
    java {
        googleJavaFormat(libs.findVersion("google-java-format").get().requiredVersion).aosp()
        target("src/**/*.java")
    }
    kotlinGradle {
        ktlint()
        target("*.gradle.kts")
    }
}
