plugins {
    id("lab.java-conventions")
}

dependencies {
    implementation(libs.hikari.cp)

    testImplementation(libs.awaitility)
}
