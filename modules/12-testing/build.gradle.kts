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

    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.testcontainers.postgresql)
    integrationTestImplementation(libs.spring.boot.testcontainers)
    integrationTestImplementation(libs.postgresql)
}
