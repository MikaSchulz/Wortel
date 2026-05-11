Open Tasks (Kurz)

Status-Übersicht
- [x] JDK 25 / Gradle toolchain konfiguriert (auto-download enabled).
- [x] application.yml erstellt und aktiv.
- [x] Projekt auf Kotlin migriert, Legacy Java entfernt.
- [ ] Paketstruktur & Features erweitern (controller/service/model) — Basis angelegt.
- [ ] CI: Build-Workflow (empfohlen).
- [ ] Tests: Unit/Integration Tests in Kotlin ergänzen.

Konkrete nächste Schritte
1) CI (High)
   - Erstelle GitHub Actions workflow mit `./gradlew clean build`.
2) Tests (Medium)
   - Portiere oder schreibe Unit-Tests in `src/test/kotlin`.
3) Compileroptions (Low)
   - Setze Kotlin `jvmTarget = "25"` in `build.gradle.kts`.

Ort der relevanten Dateien
- Migration details: `docs/MIGRATION_SUMMARY.md`
- Project guide: `docs/AGENTS.md`

Notizen zur Nachvollziehbarkeit
- Alle wichtigen Änderungen sind im Commit history enthalten (siehe Commit in `docs/MIGRATION_SUMMARY.md`).
- Falls ein Schritt rückgängig gemacht werden muss: nutze `git log`/`git checkout` für betroffene Dateien.


Detaillierter Plan (aus AI-Plan)

Kurze Checkliste (Startseite)
- [ ] 1) Lokale Umgebung prüfen & Build/Tests ausführen
- [ ] 2) Code-Qualität: Linter, Format, Abhängigkeiten prüfen
- [ ] 3) Fehlerbehebung / offene Tests fixen (falls vorhanden)
- [ ] 4) Priorisiertes Feature-Backlog erstellen (API, Spiel-Logik, UI, Persistenz)
- [ ] 5) CI/PR-Workflow einrichten (z. B. GitHub Actions)
- [ ] 6) Dokumentation & API-Dokumentation (README, `application.yml`, OpenAPI)
- [ ] 7) Release/Deployment (Docker + optional Hosting)
- [ ] 8) Monitoring, Telemetrie, Observability (optional)

Detaillierte Schritte
1) Lokale Umgebung prüfen & Build/Tests (15–30 min)
   - Ziel: Sicherstellen, dass Projekt sauber baut und Tests laufen.
   - Wichtige Dateien: `build.gradle.kts`, `src/main/resources/application.yml`, `README.md`.
   - Befehle (zsh):
     ```
     ./gradlew clean build --no-daemon
     ./gradlew test --no-daemon
     ./gradlew bootRun --no-daemon
     ```
   - Verifikation: `http://localhost:8080/health` (oder im `application.yml` konfigurierter Port).
   - Ergebnis: Liste mit Build-/Test-Fehlern (falls vorhanden).

2) Code-Qualität & Abhängigkeiten (1–3 h)
   - Tasks: Linter/Formatter prüfen (z. B. ktlint), `build.gradle.kts` auf veraltete Abhängigkeiten prüfen.
   - Dateien: `build.gradle.kts`, evtl. neue config-Dateien (`.editorconfig`, `ktlint`).
   - Ergebnis: PR mit Lint-Konfiguration + evtl. minimalen Dependency-Updates.

3) Tests & Bugfixing (1–8 h)
   - Tasks: Unit- und Integrationstests ausführen, fehlende/fehlerhafte Tests priorisieren und fixen.
   - Dateien: `src/main/kotlin/me/eyetealer/wortel/...` und `src/test/...`.
   - Ergebnis: Grüne Testsuite.

4) Feature-Backlog aufsetzen & priorisieren (30–90 min)
   - Vorschläge (priorisiert):
     1. Robustere Spiel-API: Endpunkte für neues Spiel, Rate, Spielstatus (`controller`-Layer).
     2. Persistenz optionalisieren: In-Memory -> JPA/H2/Postgres.
     3. Wortliste + Dictionary-Management (lade Wortlisten aus `resources`).
     4. API-Dokumentation: OpenAPI/Swagger (springdoc-openapi).
     5. UI (optional): einfache React/Vite frontend oder Thymeleaf für Demo.
   - Für jedes Feature: Akzeptanzkriterien, REST-Contracts, Tests.

5) CI / PR-Workflow (1–3 h)
   - Ziel: Automatisches Bauen + Tests + (optionales) Publish.
   - Vorschlag: GitHub Actions Workflow `.github/workflows/ci.yml`.
   - Ergebnis: PR-Template + CI-Pipeline.

6) Dokumentation & Developer Experience (30–90 min)
   - Aufgaben: `README.md` updaten, `application.yml` Beispiel-Profile, OpenAPI-UI erreichbar machen.
   - Dateien: `README.md`, `src/main/resources/application.yml`, `docs/`.

7) Packaging & Deployment (2–6 h)
   - Optionen: Dockerfile + optional `docker-compose.yml` mit DB.
   - Dateien: `Dockerfile`, `docker-compose.yml` (neu anzulegen).

8) Observability & Production Hardening (optional)
   - Tasks: Health checks, Micrometer/Prometheus, strukturiertes Logging.
   - Dateien: `build.gradle.kts` (dependencies), Config-Klassen.

Konkrete nächste Schritte (empfohlen)
- Sofort: 1) Lokale Build/Tests ausführen und Ergebnis melden. Ich kann das jetzt starten.
- Danach: Fixe Build-/Test-Fehler (falls vorhanden) und erst dann Feature-Implementierung.
- Priorität: Stabilität (grüne Tests) > API-Design > Persistenz > UI > CI

Optionen zum Weitermachen (bitte wählen)
- A) Führe sofort einen Build + Tests lokal im Repo aus und liefere das Ergebnis (Fehler/Erfolg + next steps).
- B) Erstelle ein GitHub Actions CI-Workflow-Template und öffne einen PR-Vorschlag (Patch).
- C) Erstelle eine kleine API-Spezifikation (OpenAPI YAML) für die gewünschten Endpunkte.
- D) Priorisiere und schreibe das initiale Issue/Backlog in `docs/OPEN_TASKS.md`.


