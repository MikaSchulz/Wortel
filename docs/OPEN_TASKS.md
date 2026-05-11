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

