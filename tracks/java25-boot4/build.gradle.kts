plugins {
    base
}

group = "lab.java25boot4"
version = "1.0.0-SNAPSHOT"

tasks.named("build") {
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("build") } })
}

tasks.register("compileBrokenExamples") {
    group = "verification"
    description = "Compiles intentionally broken review examples in the track."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("compileBrokenExamples") } })
}

tasks.register("compileExamples") {
    group = "verification"
    description = "Compiles question and demo examples in every track module."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("compileExamples") } })
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests in track modules."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("integrationTest") } })
}
