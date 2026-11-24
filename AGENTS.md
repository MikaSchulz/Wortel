AGENTS — Projekt-Guide (Kurz)

Project overview
- Wortel: small German word-guessing game, inspired by Wordle (not affiliated with The New York Times).
- Backend: Supabase (Postgres + Auth + Edge Functions in Deno/TypeScript) under `supabase/`.
- Client: Kotlin/Compose Multiplatform app (Android, iOS, Web/wasm; desktop target for tests) under `mobile/`, package `me.eyetealer.wortel`.

Key files / layout
- `supabase/functions/_shared/` — backend code, hexagonal: `domain/`, `application/`, `ports/`, `infrastructure/`, `http/`.
- `supabase/functions/games/index.ts` — HTTP entrypoint, routing + wiring only.
- `supabase/migrations/` — SQL schema + RLS policies (forward-only; never edit a deployed migration).
- `mobile/composeApp/` — KMP library with all shared code: `src/commonMain/kotlin/me/eyetealer/wortel/` (`domain/`, `data/`, `viewmodel/`, `ui/`), web entry in `wasmJsMain/`.
- `mobile/androidApp/` — Android application shell (MainActivity, manifest). AGP 9 forbids the application plugin in the KMP module.
- `wordlists/wordlist_creator.py` — generates the bundled word lists.
- `docs/openapi.yaml` — API spec. `README.md`, `supabase/README.md`, `mobile/README.md` — details.

Build & test
```bash
# Backend
cd supabase/functions
deno task verify      # fmt:check + check + lint + test

# Mobile (JDK 17+, Android SDK)
cd mobile
./gradlew :composeApp:desktopTest
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDistribution
```

Guidelines
- Follow the architecture rules in `supabase/README.md`; `_shared/architecture.test.ts` enforces them (`domain/` and `application/` stay free of Deno, fetch and Supabase imports; `http/` never imports `infrastructure/`).
- Run `deno task fmt` before committing backend code; CI checks formatting.
- The client's `domain/Evaluate.kt` mirrors the backend's `domain/game/evaluate.ts`; change both together.
- New backend error codes need a German translation in the client's `data/ErrorMessages.kt`.
- `GameViewModel` talks to interfaces (`GameApi`, `AuthService`, storage); extend the fakes in `GameViewModelTest` when they change.
- Mobile dependency versions live in `mobile/gradle/libs.versions.toml`.
- A push to `main` deploys DB migrations and then the Edge Function (`.github/workflows/deploy.yml`), and the web app to GitHub Pages.
