AGENTS — Projekt-Guide (Kurz)

Project overview
- Wortel: small German word-guessing game, inspired by Wordle (not affiliated with The New York Times).
- Backend: Supabase (Postgres + Auth + Edge Functions in Deno/TypeScript) under `supabase/`.
- Client: Kotlin/Compose Multiplatform app (Android, iOS, Web/wasm; desktop target for tests) under `mobile/`, package `me.eyetealer.wortel`.

Key files / layout
- `supabase/functions/_shared/` — backend code, hexagonal: `domain/`, `application/`, `ports/`, `infrastructure/`, `http/`.
- `supabase/functions/games/index.ts` — HTTP entrypoint, routing + wiring only.
- `supabase/migrations/` — SQL schema + RLS policies.
- `mobile/composeApp/src/commonMain/kotlin/me/eyetealer/wortel/` — shared app code (`domain/`, `data/`, `viewmodel/`, `ui/`).
- `wordlists/wordlist_creator.py` — generates the bundled word lists.
- `docs/openapi.yaml` — API spec. `README.md`, `supabase/README.md`, `mobile/README.md` — details.

Build & test
```bash
# Backend
cd supabase/functions
deno task check
deno task test
deno task lint

# Mobile (JDK 17+)
cd mobile
./gradlew :composeApp:desktopTest
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDistribution
```

Guidelines
- Follow the architecture rules in `supabase/README.md`: `domain/` stays pure (no Deno, fetch or Supabase imports).
- The client's `domain/Evaluate.kt` mirrors the backend's `domain/game/evaluate.ts`; change both together.
- Mobile dependency versions live in `mobile/gradle/libs.versions.toml`.
- A push to `main` deploys DB migrations and the Edge Function (`.github/workflows/deploy.yml`).
