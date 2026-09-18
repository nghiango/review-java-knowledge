plugins {
    `kotlin-dsl`
}

group = "lab.buildlogic"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

dependencies {
    implementation(libs.spotless.plugin)
    implementation(libs.errorprone.plugin)
}
