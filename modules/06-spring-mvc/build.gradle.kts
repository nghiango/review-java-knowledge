plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.web)
    implementation(libs.spring.webmvc)
    implementation(libs.spring.context)
    implementation(libs.jackson.databind)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)
    implementation(libs.jakarta.el)
    testImplementation(libs.spring.test)
}
