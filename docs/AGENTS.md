AGENTS — Projekt-Guide (Kurz)

Project overview
- Wortel: Kotlin-based Spring Boot application (package: `org.example.wordle`).
- Purpose: small Wordle-like demo app; skeleton ready to extend.

Key files / layout
- `build.gradle.kts` — Gradle Kotlin DSL buildscript.
- `src/main/kotlin/org/example/wordle/` — application code (controllers, services, models, util).
- `src/main/resources/application.yml` — runtime configuration.
- `README.md` (project root) — quick start and versions.

Build & run
```bash
./gradlew clean build -x test
./gradlew bootRun
# health: http://localhost:8080/health
```

Guidelines
- Use the Gradle wrapper (`./gradlew`) to ensure consistent Gradle version.
- Follow package structure: controller / service / model / util under `org.example.wordle`.
- Add dependencies in `build.gradle.kts` and prefer Maven Central.

Notes
- The project uses Kotlin 2.3.21 and Spring Boot 4.0.6.
- Java toolchain is configured for Java 25; Gradle can auto-download a matching JDK.

