plugins {
    id("lab.java25boot4-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("jspecify").get())
    implementation(libs.findLibrary("spring-context").get())
    implementation(libs.findLibrary("spring-tx").get())

    testImplementation(project(":test-support"))
    testImplementation(libs.findLibrary("spring-test").get())
}
