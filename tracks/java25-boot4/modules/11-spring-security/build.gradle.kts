plugins {
    id("lab.java25boot4-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("jspecify").get())
    implementation(libs.findLibrary("spring-context").get())
    implementation(libs.findLibrary("spring-web").get())
    implementation(libs.findLibrary("spring-webmvc").get())
    implementation(libs.findLibrary("spring-security-core").get())
    implementation(libs.findLibrary("spring-security-web").get())
    implementation(libs.findLibrary("spring-security-config").get())
    implementation(libs.findLibrary("spring-security-crypto").get())
    implementation(libs.findLibrary("jakarta-servlet-api").get())

    testImplementation(project(":test-support"))
    testImplementation(libs.findLibrary("spring-test").get())
    testImplementation(libs.findLibrary("spring-security-test").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findLibrary("junit-jupiter").get())
}
