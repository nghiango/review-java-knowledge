plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.web)
    implementation(libs.spring.data.jpa)
    implementation(libs.hibernate.core)
    implementation(libs.hikari.cp)
    implementation(libs.jakarta.persistence.api)
    implementation(libs.spring.boot)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.jackson.databind)
    implementation(libs.jakarta.validation.api)

    testImplementation(libs.spring.test)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.test.autoconfigure)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.awaitility)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.wiremock.standalone)

    // The "asserting implementation not behaviour" review target is itself a JUnit/Mockito test,
    // so the brokenExamples source set needs the test libraries to compile it. compileOnly-style
    // only: broken examples are never run and never packaged.
    add("brokenExamplesImplementation", libs.junit.jupiter)
    add("brokenExamplesImplementation", libs.mockito.core)
    add("brokenExamplesImplementation", libs.mockito.junit.jupiter)

    // The question examples are about testing, so the API under discussion *is* the test stack:
    // without these the Qnn classes could only describe JUnit, Mockito, the Spring slices,
    // Testcontainers, Awaitility, WireMock and ArchUnit instead of showing their real shape. The
    // examples source set only sees `implementation` plus `src/main` output, which is why every
    // library the examples use has to be listed here. Compile-only in effect: examples are compiled
    // by `compileExamples` and never run, packaged or published.
    add("examplesImplementation", libs.junit.jupiter)
    add("examplesImplementation", libs.mockito.core)
    add("examplesImplementation", libs.mockito.junit.jupiter)
    add("examplesImplementation", libs.assertj.core)
    add("examplesImplementation", libs.spring.boot.test.autoconfigure)
    add("examplesImplementation", libs.testcontainers.junit.jupiter)
    add("examplesImplementation", libs.testcontainers.postgresql)
    add("examplesImplementation", libs.spring.boot.testcontainers)
    add("examplesImplementation", libs.awaitility)
    add("examplesImplementation", libs.wiremock.standalone)
    add("examplesImplementation", libs.archunit.junit5)

    integrationTestImplementation(project(":modules:test-support"))
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.spring.boot.testcontainers)
    integrationTestImplementation(libs.postgresql)
}
