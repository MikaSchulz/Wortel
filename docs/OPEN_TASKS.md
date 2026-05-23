Open Tasks — Single Source of Truth

Zweck
Dieses Dokument fasst alle offenen Aufgaben, Prioritäten, den umzusetzenden Plan und den aktuellen Projektstatus zusammen. Es ersetzt die vorher separaten Dateien `docs/FEATURE_BACKLOG.md` und `docs/OPEN_TASKS.md` als zentrales Arbeitsdokument. Die alte `docs/FEATURE_BACKLOG.md` wurde auf einen Hinweis reduziert und verweist hierher.

Kurz-Status (aktuell)
- Build: ✅ `./gradlew clean build` — erfolgreich
- Tests: ✅ `./gradlew test` — erfolgreich (Unit-Tests grün)
- Code-Qualität: Spotless (textbasierte Regeln) konfiguriert in `build.gradle.kts` (Plugin `com.diffplug.spotless`)
- Letzte Änderungen (lokal angewendet): `build.gradle.kts` (Spotless), `docs/FEATURE_BACKLOG.md` (initial), `docs/OPEN_TASKS.md` (wurde hierher gemerged)

Master-Checkliste (Startseite)
- [x] 1) Lokale Umgebung prüfen & Build/Tests ausführen
- [x] 2) Code-Qualität: Linter/Formatter initial einrichten
- [x] 3) Tests & Bugfixing
- [x] 4) Priorisiertes Feature-Backlog anlegen
- [x] 5) CI/PR-Workflow einrichten (GitHub Actions)
- [x] 6) P0-Implementation: REST-API (Spring Boot, mittlerweile in `_archive_spring/`)
- [x] 7) Architektur-Migration auf Supabase (Edge Functions Deno/TS + Postgres + Auth)
- [x] 8) RLS-Policies + Schema-Migration
- [x] 9) Deno-Unit-Tests + Type-Check + CI angepasst
- [ ] 10) Mobile-App (Kotlin Multiplatform — iOS + Android)
- [ ] 11) Supabase-Projekt anlegen, `supabase link`, `supabase db push`, `supabase functions deploy games`
- [ ] 12) Daily-Word (Cron im Edge-Runtime oder Postgres `pg_cron`)
- [ ] 13) Statistiken-Tabelle (Streaks, gewonnene Spiele pro User)
- [ ] 14) Optional: Wortlisten-Admin-Endpunkt + Liste in DB statt Function-Bundle

Detaillierter Plan (Schritte & Details)

1) Lokale Umgebung prüfen & Build/Tests (15–30 min)
   - Ziel: Sicherstellen, dass das Projekt sauber baut und Tests laufen.
   - Wichtige Dateien: `build.gradle.kts`, `src/main/resources/application.yml`, `README.md`.
   - Empfohlene Befehle (zsh):
     ```
     ./gradlew clean build --no-daemon
     ./gradlew test --no-daemon
     ./gradlew bootRun --no-daemon
     ```
   - Verifikation: Health-Endpoint (z. B.) `http://localhost:8080/health`
   - Ergebnis: Liste mit Build-/Test-Fehlern (falls vorhanden). Aktuell: Build und Tests grün.

2) Code-Qualität & Abhängigkeiten (1–3 h)
   - Tasks:
     - Formatter/Linter: `Spotless` mit textbasierten Regeln konfiguriert. Später optional `ktlint`/`ktfmt` (abhängig von Kompatibilität).
     - `build.gradle.kts` auf veraltete/unsichere Abhängigkeiten prüfen.
   - Dateien: `build.gradle.kts`, ggf. `.editorconfig`, `ktlint`-Konfig.
   - Ergebnis: PR mit Lint/Formatter-Konfiguration + evtl. Dependency-Updates.

3) Tests & Bugfixing (1–8 h)
   - Tasks:
     - Unit- und Integrationstests ausführen; fehlende/fehlerhafte Tests priorisieren und fixen.
     - Sicherstellen, dass Tests deterministisch laufen (keine flakiness).
   - Dateien: `src/main/kotlin/me/eyetealer/wortel/...`, `src/test/...`.
   - Ergebnis: Grüne Testsuite.

4) Feature-Backlog aufsetzen & priorisieren (30–90 min)
   - P0 (High) — sofortige Priorität:
     - POST /api/v1/games — neues Spiel erstellen
       - Rückgabe: Spiel-ID, Wortlänge, maxVersuche
       - Tests: Unit + Integration
     - GET /api/v1/games/{id} — Spielstatus abfragen
       - Rückgabe: Status (running/won/lost), bisherige Versuche, verbleibende Versuche
     - Spiele-Logik: Rate-Mechanik (correct/present/absent), Handling von doppelten Buchstaben

   - P1 (Medium):
     - Persistenz: In-Memory -> optional JPA + H2/Postgres
     - Wortliste-Management: Lade Wortlisten aus `src/main/resources/words/`
     - OpenAPI/Swagger (springdoc-openapi)

   - P2 (Low):
     - UI: Demo-Frontend (Thymeleaf oder React/Vite)
     - CI/CD: GitHub Actions (build + test)

5) CI / PR-Workflow (1–3 h)
   - Empfohlen: `.github/workflows/ci.yml` mit Schritten: checkout, setup JDK (Toolchain-kompatibel), `./gradlew clean build`, `./gradlew test`, `./gradlew spotlessCheck`.

6) Dokumentation & Developer Experience (30–90 min)
   - README updaten (Run/Dev/Ports)
   - `application.yml` Beispiel-Profile (`dev`, `prod`)
   - API-Dokumentation (OpenAPI) bereitstellen

7) Packaging & Deployment (2–6 h)
   - Dockerfile für App + optional `docker-compose.yml` (DB)

8) Observability & Production Hardening (optional)
   - Micrometer, Prometheus, structured logging, Health/Readiness checks

Konkrete, kurzfristige Aktionen (Empfohlen)
- Sofort: CI-Workflow anlegen (GitHub Actions) — ich kann die Datei erstellen.
- Alternativ sofort: API-Spec (OpenAPI) für P0-Endpunkte erstellen.
- Mittelfristig: Implementiere P0-API-Endpunkte + Tests in einem Feature-Branch (`feature/api-games`).

Aktuelle Änderungen im Repo (lokal vorgenommen)
- `build.gradle.kts`: Spotless-Plugin & sichere textbasierte Regeln hinzugefügt
- `docs/FEATURE_BACKLOG.md`: initiales Backlog (wurde hierher gemerged)
- `docs/OPEN_TASKS.md`: dieses Dokument (konzolidiert)

Hinweis / Aufräumen
- Die separate Datei `docs/FEATURE_BACKLOG.md` wurde durch eine Verweisdatei ersetzt; alle Inhalte sind jetzt hier im zentralen Dokument.

Wie du weitermachen kannst (Optionen)
- A) Ich erstelle die GitHub Actions CI-Datei `.github/workflows/ci.yml` und mache einen PR-Branch.
- B) Ich schreibe eine OpenAPI YAML/JSON für die P0-Endpunkte.
- C) Ich scaffolde die P0-Implementation (Controller + Service + Unit/Integration Tests) in einem Feature-Branch.
- D) Du editierst dieses Dokument direkt — ich übernehme Änderungen in PRs oder Branches.

Wenn du möchtest, mache ich jetzt direkt A, B oder C. Sag mir, welche Option du bevorzugst.

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


