plugins {
    id("lab.java25boot4-conventions")
}

dependencies {
    api(libs.testcontainers)
    api(libs.testcontainers.junit)
    api(libs.testcontainers.postgresql)
    api(libs.assertj.core)
    api(libs.junit.jupiter)
}
