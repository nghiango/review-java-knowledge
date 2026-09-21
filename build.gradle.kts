plugins {
    base
}

group = "lab"
version = "1.0.0-SNAPSHOT"

tasks.named("build") {
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("build") } })
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests in every code module."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("integrationTest") } })
}

tasks.register("compileBrokenExamples") {
    group = "verification"
    description = "Compiles intentionally broken review examples without adding them to build."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("compileBrokenExamples") } })
}

tasks.register("compileExamples") {
    group = "verification"
    description = "Compiles question and demo examples in every code module."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("compileExamples") } })
}

val buildTrackJava25Boot4 = tasks.register("buildTrack-java25-boot4") {
    group = "verification"
    description = "Builds the java25-boot4 track included build."
    dependsOn(gradle.includedBuild("java25-boot4").task(":build"))
    dependsOn(gradle.includedBuild("java25-boot4").task(":compileBrokenExamples"))
}

tasks.register("buildTracks") {
    group = "verification"
    description = "Builds all language and version tracks."
    dependsOn(buildTrackJava25Boot4)
}
