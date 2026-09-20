plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.aspectjweaver)
    testImplementation(libs.spring.test)
}
