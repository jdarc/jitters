import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.0-RC"
    application
}

repositories { mavenCentral() }

application { mainClass.set("ProgramKt") }

tasks.withType<KotlinCompile> {
    kotlinOptions {
        version = 16
        jvmTarget = "16"
        languageVersion = "1.5"
    }
}
