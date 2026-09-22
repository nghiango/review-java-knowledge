plugins {
    id("lab.java25boot4-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("jspecify").get())
    implementation(libs.findLibrary("spring-context").get())
    implementation(libs.findLibrary("spring-boot").get())
    implementation(libs.findLibrary("jakarta-validation-api").get())
    implementation(libs.findLibrary("hibernate-validator").get())
    implementation(libs.findLibrary("jakarta-el").get())

    testImplementation(project(":test-support"))
    testImplementation(libs.findLibrary("spring-test").get())
    testImplementation(libs.findLibrary("spring-boot-test").get())
}

tasks.register<JavaExec>("runFeatureTour") {
    group = "application"
    description = "Runs the runnable Java 25 / Boot 4 feature tour."
    mainClass.set("lab.java25boot4.whatsnew.FeatureTour")
    classpath = sourceSets.named("main").get().runtimeClasspath
    jvmArgs("--enable-preview")
}
