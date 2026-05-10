import org.gradle.api.tasks.SourceSetContainer
import java.io.File

plugins {
    id("java")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Note: Kotlin compiler options use the newer compilerOptions DSL in Kotlin 2.x.
// We avoid setting deprecated kotlinOptions here to remain compatible with the
// Kotlin 2.4.0-Beta2 plugin. If you want explicit jvmTarget/flags, we can add
// the compilerOptions DSL (requires imports). Leave defaults for now.

repositories {
    mavenCentral()
}

// Remove Java sources from the default Java sourceSet so only Kotlin sources
// under src/main/kotlin are compiled. Legacy Java files have been moved to
// src/legacy_java and will be ignored by the build.
the<SourceSetContainer>().named("main") {
    java.setSrcDirs(emptyList<File>())
}

// Explicit main class to avoid ambiguous main-class detection between Java and Kotlin
springBoot {
    mainClass.set("org.example.wordle.WordleApplicationKt")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:4.0.6")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.21")

    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.6")
}

tasks.test {
    useJUnitPlatform()
}