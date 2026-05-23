Wortel — README
================

Wortle-Backend auf Supabase (Postgres + Auth + Edge Functions in Deno/TypeScript).

## Stack

- **Supabase Postgres** — Spielzustand (`public.games`) mit Row Level Security
- **Supabase Auth** — Email + anonymes Sign-in; JWT in Edge Functions validiert
- **Edge Functions** — Deno/TypeScript, Funktion `games` exposed REST-API
- **Wortlisten** — gebündelt in der Function unter `supabase/functions/_shared/data/`

## Quick start

```bash
# Deno installieren
curl -fsSL https://deno.land/install.sh | sh

# Supabase CLI installieren
brew install supabase/tap/supabase   # oder: scoop/npm install -g supabase

# Lokalen Stack starten (Postgres + Auth + Edge runtime)
supabase start
supabase db reset           # wendet supabase/migrations/* an

# Edge Function lokal serven
supabase functions serve games --env-file ./supabase/.env.local

# Tests
cd supabase/functions
deno task test
deno task check
```

Smoke test:

```bash
curl -X POST http://127.0.0.1:54321/functions/v1/games \
  -H 'Content-Type: application/json' \
  -d '{"wordLength":5,"maxAttempts":6}'
```

## API

Base URL nach Deploy: `https://<project-ref>.supabase.co/functions/v1/games`

- `POST /games` → erstellt Spiel
- `GET /games/{id}` → Spielstatus
- `POST /games/{id}/guesses` → rät

`Authorization: Bearer <jwt>` setzt das Spiel auf den User. Ohne Header → anonymes Spiel.

Details: [`supabase/README.md`](supabase/README.md) und [`docs/openapi.yaml`](docs/openapi.yaml).

## Verzeichnislayout

```
Wortel/
├── supabase/
│   ├── config.toml
│   ├── migrations/        # SQL: tables + RLS
│   ├── functions/
│   │   ├── _shared/       # Wortliste, Auth, CORS, Spielmechanik (testbar)
│   │   └── games/         # Deno.serve mit interner Routing
│   └── README.md
├── wordlists/
│   └── wordlist_creator.py    # Python: deutsche FrequencyWords-Quelle
├── docs/
│   ├── openapi.yaml
│   └── OPEN_TASKS.md
└── _archive_spring/       # vorherige Spring-Boot-Implementation, ungenutzt
```

## Deploy zu Supabase Cloud

Standard-Pfad: GitHub Actions auf push zu `main`.

Pipeline: `verify (deno check/test/lint) → db push → functions deploy → smoke test`.

One-time setup (Repo Secrets):
- `SUPABASE_ACCESS_TOKEN` — Dashboard → Account → Access Tokens
- `SUPABASE_PROJECT_REF` — Project-URL Slug
- `SUPABASE_DB_PASSWORD` — bei Projekt-Erstellung gesetzt
- `SUPABASE_PUBLISHABLE_KEY` — Project → Settings → API Keys → Publishable (`sb_publishable_*`)
- `WORTEL_CORS_ALLOWED_ORIGINS` — optional

Hinweis: Supabase hat Ende 2025 Keys umbenannt. Legacy `anon public` → `sb_publishable_*`, Legacy `service_role` → `sb_secret_*`. Code unterstützt beide.

Manuell triggern: GitHub → Actions → "Deploy to Supabase" → Run workflow.

Manual CLI als Fallback:
```bash
supabase login
supabase link --project-ref <project-ref>
supabase db push
supabase functions deploy games
```

Vollständige Doku: [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

## Tests & CI

- Unit-Tests (Deno): `cd supabase/functions && deno task test`
- Type-Check: `deno task check`
- CI: `.github/workflows/ci.yml` (Deno-basiert, läuft auf push/PR)

## Legacy Spring-Boot

`_archive_spring/` enthält den vorherigen Kotlin/Spring-Stack als Referenz.
Wird nicht mehr deployed.
