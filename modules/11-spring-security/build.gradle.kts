plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.web)
    implementation(libs.spring.webmvc)
    implementation(libs.spring.security.core)
    implementation(libs.spring.security.web)
    implementation(libs.spring.security.config)
    implementation(libs.spring.security.crypto)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)
    implementation(libs.jakarta.el)
    implementation(libs.jackson.databind)

    testImplementation(libs.spring.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
