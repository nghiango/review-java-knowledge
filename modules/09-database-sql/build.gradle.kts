plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.spring.jdbc)
    implementation(libs.hikari.cp)
    implementation(libs.jackson.databind)
    implementation(libs.jakarta.annotation.api)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
