import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0-M2"
    application
}

repositories { mavenCentral() }

application { mainClass.set("ProgramKt") }

tasks.withType<KotlinCompile> {
    kotlinOptions {
        useIR = true
        jvmTarget = "11"
        languageVersion = "1.5"
        freeCompilerArgs = listOf("-Xinline-classes")
    }
}
