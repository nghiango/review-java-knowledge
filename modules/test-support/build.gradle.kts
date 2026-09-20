plugins {
    id("lab.java-conventions")
    `java-library`
}

dependencies {
    // Declared with `api` (not `implementation`) on purpose: consumers inherit Testcontainers and
    // the PostgreSQL module from this project, so a module's integrationTest source set only has to
    // depend on `:modules:test-support` to use the shared containers.
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.kafka)
    api(libs.testcontainers.rabbitmq)
}
