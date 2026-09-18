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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs Docker-backed and full-context integration tests."
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.named("test"))
}

tasks.register("compileBrokenExamples") {
    group = "verification"
    description = "Compiles intentionally flawed code without running it or adding it to build."
    dependsOn(tasks.named(brokenExamples.compileJavaTaskName))
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
