# Feature Backlog (priorisiert)

Dieses Dokument enthält einen initial priorisierten Backlog für das Wortel-Projekt. Ergänze/ändere Items direkt in dieser Datei.

## Priorität: P0 (High)

- API: Neues Spiel erstellen
  - Endpunkt: POST /api/v1/games
  - Akzeptanzkriterien: erstellt ein neues Spiel, liefert Spiel-ID, Wortlänge, maxVersuche
  - Tests: Unit + Integration

- API: Spielstatus abfragen
  - Endpunkt: GET /api/v1/games/{id}
  - Akzeptanzkriterien: Status (running/won/lost), bisherige Versuche, verbleibende Versuche
  - Tests: Unit + Integration

- Spiele-Logik: Rate-Mechanik
  - Implementiere Match- und Position-Feedback (correct/present/absent)
  - Tests: umfangreiche Unit-Tests für Randfälle (duplicate letters etc.)

## Priorität: P1 (Medium)

- Persistenz: Optionaler DB-Support
  - In-Memory (aktuell) weiter unterstützen; optional JPA + H2/Postgres
  - Migrationen mit Flyway (optional)

- Wortliste-Management
  - Lade Wortlisten aus `src/main/resources/words/` und stelle Validierung bereit
  - CLI / Admin-Endpoint zum Nachladen

- OpenAPI / Swagger
  - Springdoc OpenAPI integrieren, Swagger UI unter `/swagger-ui.html`

## Priorität: P2 (Low)

- UI: kleine Demo-Weboberfläche
  - Thymeleaf oder separates React/Vite-Frontend

- CI/CD: GitHub Actions Workflow (build + tests)

## Offene Ideen / Nice-to-have

- Metrics: Micrometer + Prometheus
- Rate-Limiting für API-Endpunkte
- Feature-Flags für Spielvarianten

---

Ändere die Priorität oder füge Issues/Tasks hinzu — ich kann daraus automatisch Issues/Branches generieren, wenn du möchtest.

