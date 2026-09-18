plugins {
    base
}

group = "lab"
version = "1.0.0-SNAPSHOT"

gradle.projectsEvaluated {
    tasks.named("build") {
        dependsOn(subprojects.map { it.tasks.named("build") })
    }
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
