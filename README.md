Wortel — Kurz-README
====================

Kurz: minimal lauffähiges Wortle-Projekt, vollständig auf Kotlin migriert.

Quick start

```zsh
cd /Users/mikaschulz/Documents/Code/Wortel/Wortel
./gradlew clean build -x test
./gradlew bootRun
# open http://localhost:8080/health
```

Wichtige Versionen
- Kotlin: 2.3.21
- Spring Boot: 4.0.6
- Gradle Wrapper: 9.3.0
- Java Toolchain: 25 (Gradle auto-download enabled)

Projektstruktur (wichtigste Orte)
- `build.gradle.kts` — Buildscript (Kotlin DSL)
- `src/main/kotlin/` — alle Produktionsquellen (Paket: `org.example.wordle`)
- `src/main/resources/application.yml` — zentrale Konfiguration
- `docs/` — ausführliche Migrations- und Aufgaben-Dokumente (siehe unten)

Weitere Hinweise
- Legacy Java-Quellen wurden entfernt; das Projekt baut und startet rein mit Kotlin.
- `.idea/` ist in `.gitignore` und sollte aus dem Git-Index entfernt werden (`git rm -r --cached .idea`).

Weitere Dokumente
- Detaillierte Hinweise und Nachverfolgung findest du im `docs/`-Ordner:
  - `docs/AGENTS.md` — Projekt-Guide (Kurzversion)
  - `docs/MIGRATION_SUMMARY.md` — Zusammenfassung der Migration
  - `docs/OPEN_TASKS.md` — offene Aufgaben / Prioritäten

Wenn du möchtest, kann ich CI-Workflow oder explizite Kotlin compilerOptions (z. B. `jvmTarget = "25"`) hinzufügen.
