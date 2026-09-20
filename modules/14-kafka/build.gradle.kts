plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.spring.kafka)
    implementation(libs.kafka.clients)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jakarta.annotation.api)

    testImplementation(libs.spring.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.awaitility)

    integrationTestImplementation(project(":modules:test-support"))
    integrationTestImplementation(libs.spring.test)
    integrationTestImplementation(libs.spring.kafka.test)
    integrationTestImplementation(libs.spring.boot.test)
    integrationTestImplementation(libs.spring.boot.testcontainers)
    integrationTestImplementation(libs.spring.boot.test.autoconfigure)
    integrationTestImplementation(libs.awaitility)
}
