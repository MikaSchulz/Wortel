Migration Summary

Datum: 2026-05-11
Commit: 6ddacd6d877089f82b5a72b34e062002ab07ffdd

Kurz:
- Projekt auf Kotlin migriert (stabile Kotlin 2.3.21).
- Spring Boot auf 4.0.6 gesetzt.
- Gradle wrapper: 9.3.0
- Java toolchain: target = 25; `org.gradle.java.installations.auto-download=true` in `gradle.properties`.
- Legacy Java sources removed from `src/main/java` and repository.

Wichtige Dateien
- `build.gradle.kts` — Kotlin plugins & dependencies, toolchain configuration.
- `src/main/kotlin/...` — aktive Quellcode-Dateien (application, controller, service, model, util).
- `src/main/resources/application.yml` — runtime configuration.

Wie prüfen / zurücksetzen
- Build & Run:
  - `./gradlew clean build -x test`
  - `./gradlew bootRun`
- Alte Java-Dateien können aus der Git-History wiederhergestellt werden (z. B. `git checkout <old-commit> -- <path>`).

Empfohlene nächste Schritte (kurz)
- CI (GitHub Actions) hinzufügen, der `./gradlew build` prüft.
- Optional: explizit Kotlin compilerOptions (z. B. `jvmTarget = "25"`) in `build.gradle.kts` setzen.
- Tests nach Kotlin migrieren / erweitern.

