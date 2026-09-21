plugins {
    id("lab.java25boot4-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("jspecify").get())
    implementation(libs.findLibrary("spring-context").get())
    implementation(libs.findLibrary("spring-web").get())
    implementation(libs.findLibrary("spring-webmvc").get())
    implementation(libs.findLibrary("jakarta-servlet-api").get())
    implementation(libs.findLibrary("jakarta-validation-api").get())
    implementation(libs.findLibrary("hibernate-validator").get())
    implementation(libs.findLibrary("jakarta-el").get())
    implementation(libs.findLibrary("jackson-databind").get())

    testImplementation(project(":test-support"))
    testImplementation(libs.findLibrary("spring-test").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findLibrary("junit-jupiter").get())
}
