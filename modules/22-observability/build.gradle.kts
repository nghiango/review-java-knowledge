plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.observation)
    implementation(libs.micrometer.tracing)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.jackson.databind)

    testImplementation(libs.spring.test)
    testImplementation(libs.micrometer.observation.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.awaitility)

    integrationTestImplementation(libs.spring.test)
    integrationTestImplementation(libs.mockito.core)
    integrationTestImplementation(libs.mockito.junit.jupiter)
    integrationTestImplementation(libs.awaitility)
}
