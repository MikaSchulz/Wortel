# Migration & Cleanup Summary

Datum: 2026-05-11
Commit: 6ddacd6d877089f82b5a72b34e062002ab07ffdd

Kurz: Ich habe das Projekt lauffähig gemacht, auf Kotlin migriert, den Build aktualisiert
und alle Legacy-Java-Quellen aus dem Build entfernt. Die App startet lokal mit Spring Boot.

Wesentliche Änderungen
- Kotlin: Projekt auf Kotlin (stable) migriert. Verwendete Kotlin-Version: `2.3.21`.
- Spring Boot: `4.0.6` (in `build.gradle.kts`).
- Java: Gradle toolchain auf Java 25 (languageVersion = 25) konfiguriert.
- Gradle Wrapper: Das Repo verwendet `gradle-9.3.0` (siehe `gradle/wrapper/gradle-wrapper.properties`).
- Legacy Java: Alle Java-Quellen wurden aus dem Build entfernt und vollständig aus den `src/main/java` und `src/legacy_java` Verzeichnissen gelöscht.

Wichtigste neue/aktualisierte Dateien
- `build.gradle.kts` — Kotlin-Plugins hinzugefügt, Kotlin-Dependencies auf 2.3.21 gesetzt, Toolchain auf Java 25, `springBoot.mainClass` gesetzt.
- `src/main/kotlin/...` — alle aktiven Quellklassen jetzt in Kotlin:
  - `org.example.wordle.WordleApplication.kt`
  - `org.example.wordle.util.IO.kt`
  - `org.example.wordle.util.Main.kt`
  - `org.example.wordle.controller.HealthController.kt`
  - `org.example.wordle.service.GameService.kt`
  - `org.example.wordle.model.WordleState.kt`
- `src/main/resources/application.yml` — Basis-Konfiguration (server.port, logging, app name).
- `OPEN_TASKS.md` — Übersicht offener Aufgaben (im Repo).

Was funktioniert / wie testen
1. Build:
```bash
./gradlew clean build -x test
```

2. Starten:
```bash
./gradlew bootRun
# dann prüfen: http://localhost:8080/health -> sollte "OK" zurückgeben
```

Hinweise zur Wiederherstellung/History
- Die entfernten Java-Dateien sind endgültig aus dem Haupt-Quellbaum gelöscht and in git history vorhanden. Falls du die alten Java-Dateien wiederherstellen willst, kannst du sie aus dem Commit-History wiederherstellen, z. B.:
  ```bash
  git checkout HEAD~1 -- src/main/java
  ```
  oder gezielt eine Datei aus dem Commit, der sie noch enthielt.

Empfohlene nächste Schritte (optional)
- (A) `compilerOptions` DSL für Kotlin 2.x konfigurieren, z. B. explizit `jvmTarget = "25"`.
- (B) CI-Workflow (z. B. GitHub Actions) hinzufügen, der `./gradlew build` prüft.
- (C) Tests in Kotlin ergänzen/migrieren.
- (D) Optional: Entferne nicht mehr benötigte Konfiguration/Kommentare und passe README an.

Kontaktpunkte für zukünftige Sessions
- Gradle-Version: `9.3.0`
- Kotlin-Version: `2.3.21`
- Spring Boot-Version: `4.0.6`
- Java-Toolchain: `25`

Wenn du die Session schließt, sind diese Informationen und die Änderungen im Repository (Commit oben) gespeichert — beim nächsten Öffnen der Repo kann ich darauf aufbauen.

