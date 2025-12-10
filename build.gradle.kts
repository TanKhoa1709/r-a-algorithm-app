plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
}

allprojects {
    group = "com.ricart.agrawala"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    dependencies {
        val implementation by configurations
        implementation(kotlin("stdlib"))
        
        // Testing
        val testImplementation by configurations
        val testRuntimeOnly by configurations
        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}