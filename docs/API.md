API Quickstart

This file documents the planned P0 API endpoints and examples to test them locally.

Planned endpoints (see also `docs/openapi.yaml`):

- POST /api/v1/games
  - Create a new game. Optional body: `{ "wordLength": 5, "maxAttempts": 6 }`.
  - Response (201): `{ "id": "<uuid>", "wordLength":5, "maxAttempts":6 }`.

- GET /api/v1/games/{id}
  - Get a game's current status. Response (200):
    ```json
    {
      "id":"<id>",
      "status":"running",
      "attempts":[
        { "guess":"crate", "result": ["absent","present","absent","correct","absent"] }
      ],
      "remainingAttempts": 4
    }
    ```

Quick curl examples (after `./gradlew bootRun`):

```bash
# create a new game
curl -v -X POST http://localhost:8080/api/v1/games -H 'Content-Type: application/json' -d '{"wordLength":5}'

# get game status
curl -v http://localhost:8080/api/v1/games/<gameId>
```

Notes
- This is a static API design document (OpenAPI) to help implementing controllers and services.
- If you want runtime Swagger UI, we can add `springdoc-openapi` to `build.gradle.kts` and a small config class. I avoided adding runtime dependencies to keep changes low-risk for now.
