Wortel — README
================

Wortel ist ein deutsches Wortratespiel, angelehnt an Wordle. Das Repo enthält das
Backend auf Supabase (Postgres + Auth + Edge Functions in Deno/TypeScript) und die
Client-App in Kotlin/Compose Multiplatform (Android, iOS, Web).

> Wortel ist ein unabhängiges Hobbyprojekt und steht in keiner Verbindung zu
> The New York Times Company. Es wird von ihr weder unterstützt noch gesponsert.
> „Wordle“ ist eine Marke von The New York Times Company.

**Live**
- Web-App: `https://mikaschulz.github.io/Wortel/`
- API: `https://lmmjbpgfnuxbcdvjtdkm.supabase.co/functions/v1/games` — curl-Beispiele in [`docs/SHOWCASE.md`](docs/SHOWCASE.md)

## Stack

| Teil | Technik |
| --- | --- |
| Datenbank | Supabase Postgres — `public.games`, `public.daily_challenges`, Row Level Security |
| Auth | Supabase Auth — Google OAuth + anonymer Gast-Login; JWT-Prüfung in der Edge Function (`auth.getClaims`) |
| API | Edge Function `games` (Deno 2, TypeScript), hexagonale Architektur |
| Wortlisten | 4–7 Buchstaben, als TS-Module gebündelt unter `supabase/functions/_shared/infrastructure/data/` |
| Client | Kotlin 2.4, Compose Multiplatform 1.12, Ktor 3, supabase-kt 3 |

## Verzeichnislayout

```
Wortel/
├── supabase/
│   ├── config.toml
│   ├── migrations/            # SQL: Tabellen, RLS, Indizes
│   ├── functions/
│   │   ├── _shared/           # domain / application / ports / infrastructure / http
│   │   └── games/index.ts     # Composition Root: Routing + Wiring
│   └── README.md              # Backend-Architektur + Regeln
├── mobile/
│   ├── composeApp/            # KMP-Library: gesamte App-Logik + UI (alle Plattformen), Web-Entry
│   ├── androidApp/            # Android-App-Hülle (MainActivity)
│   └── README.md
├── wordlists/                 # Python-Generator für die Wortlisten
└── docs/
    ├── openapi.yaml           # API-Spezifikation
    ├── DEPLOYMENT.md
    ├── SHOWCASE.md
    └── OPEN_TASKS.md
```

## API

| Methode | Pfad | Zweck |
| --- | --- | --- |
| POST | `/games` | Freies Spiel anlegen (`wordLength` 4–7, `maxAttempts` 1–20) |
| GET | `/games/{id}` | Spielstand |
| POST | `/games/{id}/guesses` | Raten |
| POST | `/games/{id}/hint` | Tipp-Wort holen |
| GET | `/games/daily` | Metadaten der Tageschallenge |
| POST | `/games/daily` | Tageschallenge spielen (idempotent) |

`Authorization: Bearer <jwt>` bindet das Spiel an den User; ohne gültiges JWT ist das
Spiel anonym. Details: [`supabase/README.md`](supabase/README.md), [`docs/openapi.yaml`](docs/openapi.yaml).

## Quick start

```bash
# Backend: Deno 2 + Supabase CLI 3 installieren, dann
supabase start                      # lokaler Stack (Docker), wendet Migrationen an
supabase functions serve games --env-file ./supabase/.env.local
cd supabase/functions && deno task verify   # fmt + typecheck + lint + tests

# Client (JDK 21, Android SDK)
cd mobile
./gradlew :composeApp:desktopTest
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web-App lokal
```

## Deploy

Push auf `main` → GitHub Actions:

- `deploy.yml`: verify → `supabase db push` → `supabase functions deploy games` → Smoke-Test
- `web-deploy.yml`: Wasm-Build → GitHub Pages
- `ci.yml` / `mobile-build.yml`: Tests, Lint, Migrations-Check, APK auf jedem PR

Benötigte Repo-Secrets und Einrichtung: [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).
