import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0-M1"
    application
}

repositories { mavenCentral() }

application {
    mainClass.set("ProgramKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.5"
        freeCompilerArgs = listOf("-Xinline-classes")
        useIR = true
    }
}
