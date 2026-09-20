plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.web)
    implementation(libs.spring.webflux)
    implementation(libs.spring.jdbc)
    implementation(libs.reactor.core)
    implementation(libs.jackson.databind)
    implementation(libs.jakarta.annotation.api)

    testImplementation(libs.spring.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.awaitility)

    integrationTestImplementation(libs.wiremock.standalone)
    integrationTestImplementation(libs.spring.test)
    integrationTestImplementation(libs.reactor.test)
    integrationTestImplementation(libs.awaitility)
}
