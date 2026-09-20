plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.tx)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.context)
    implementation(libs.hikari.cp)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.aspectjweaver)
    testImplementation(libs.spring.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
