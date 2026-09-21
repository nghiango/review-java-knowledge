plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)
    implementation(libs.jakarta.annotation.api)
    compileOnly(libs.jakarta.persistence.api)

    testImplementation(libs.archunit.junit5)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
