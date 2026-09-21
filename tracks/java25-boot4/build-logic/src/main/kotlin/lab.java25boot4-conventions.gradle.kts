import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
    id("com.diffplug.spotless")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val sourceSets = extensions.getByType<SourceSetContainer>()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    add("testImplementation", libs.findLibrary("junit-jupiter").get())
    add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
    add("testImplementation", libs.findLibrary("assertj-core").get())
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs Docker-backed and full-context integration tests for Java 25 track."
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    jvmArgs("--enable-preview")
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
    options.compilerArgs.addAll(listOf("-parameters", "--enable-preview"))
}

extensions.configure<SpotlessExtension> {
    java {
        googleJavaFormat("1.28.0").aosp()
        target("src/**/*.java")
        // google-java-format 1.28.0 cannot parse Java 25 preview syntax (unnamed patterns,
        // primitive type patterns), so these examples are left unformatted.
        targetExclude("**/*Unnamed*.java", "**/*Primitive*Pattern*.java")
    }
    kotlinGradle {
        ktlint()
        target("*.gradle.kts")
    }
}
