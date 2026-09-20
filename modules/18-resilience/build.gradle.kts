plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.web)
    implementation(libs.jackson.databind)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.bulkhead)
    implementation(libs.resilience4j.timelimiter)

    testImplementation(libs.spring.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.awaitility)

    integrationTestImplementation(libs.wiremock.standalone)
    integrationTestImplementation(libs.spring.test)
    integrationTestImplementation(libs.awaitility)
}
