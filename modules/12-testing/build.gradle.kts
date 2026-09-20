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

    // The question examples are about testing, so the API under discussion is JUnit, Mockito and
    // AssertJ itself. The examples source set only sees `implementation` plus `src/main` output, so
    // without these the Qnn classes could not show a single assertion or stub. Compile-only in
    // effect: examples are compiled by `compileExamples` and never run, packaged or published.
    add("examplesImplementation", libs.junit.jupiter)
    add("examplesImplementation", libs.mockito.core)
    add("examplesImplementation", libs.assertj.core)

    integrationTestImplementation(project(":modules:test-support"))
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.spring.boot.testcontainers)
    integrationTestImplementation(libs.postgresql)
}

// Testcontainers 1.20.6 pins docker-java's Docker API version to 1.32 whenever the user has not
// chosen one, and this environment's Docker Engine (29.x) rejects any request below API 1.40 with
// HTTP 400. Pin the API version for the container tests to the lowest one Docker >= 19.03 serves,
// so the suite works here and on older daemons alike. docker-java reads this as the `api.version`
// system property; Testcontainers keeps a user-supplied version instead of overriding it.
tasks.named<Test>("integrationTest") {
    systemProperty("api.version", "1.40")
}
