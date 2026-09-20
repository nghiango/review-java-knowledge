plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.spring.boot)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.actuator.autoconfigure)
    implementation(libs.spring.context)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.hibernate.validator)
    implementation(libs.jakarta.el)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.test)
}
