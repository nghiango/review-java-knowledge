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
