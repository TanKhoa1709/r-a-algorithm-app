plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core"))
    implementation(project(":network"))
    implementation(compose.desktop.currentOs)
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-cio:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

application {
    mainClass.set("app.visualizer.MainKt")
}
