# Wortel on Supabase

Backend for the Wortel (Wordle) game implemented as Supabase Edge Functions + Postgres + Auth.

## Stack

- **Postgres** — game state (`public.games`) with Row Level Security
- **Auth** — Supabase Auth (email/anon); JWT validated inside Edge Functions
- **Edge Functions** — Deno/TypeScript: `games` function exposes REST API
- **Wordlists** — bundled inside the function at `functions/_shared/data/`

## Directory layout (Hexagonal / Ports & Adapters)

```
supabase/
├── config.toml
├── migrations/
│   ├── 20260520120000_init_games.sql      # games table + RLS
│   └── 20260520130000_games_version.sql   # optimistic locking column
├── functions/
│   ├── deno.json
│   ├── _shared/
│   │   ├── domain/                # PURE: no DB, no HTTP, no fetch
│   │   │   ├── game/
│   │   │   │   ├── Game.ts        # aggregate root — all invariants
│   │   │   │   ├── GameStatus.ts
│   │   │   │   ├── LetterResult.ts
│   │   │   │   ├── Guess.ts
│   │   │   │   ├── evaluate.ts    # two-pass scoring
│   │   │   │   └── Game.test.ts
│   │   │   └── shared/DomainError.ts
│   │   ├── application/           # use cases: orchestrate domain + ports
│   │   │   ├── CreateGame.ts
│   │   │   ├── GetGame.ts
│   │   │   ├── SubmitGuess.ts
│   │   │   └── SubmitGuess.test.ts
│   │   ├── ports/                 # interfaces — domain's contract with outside
│   │   │   ├── GameRepository.ts
│   │   │   └── WordRepository.ts
│   │   ├── infrastructure/        # adapter impls: Supabase, file system
│   │   │   ├── SupabaseGameRepository.ts
│   │   │   ├── FileWordRepository.ts
│   │   │   ├── supabaseClient.ts
│   │   │   └── data/              # bundled wordlists (5/6/7 letters)
│   │   └── http/                  # adapter: CORS, auth, validation, DTOs, error map
│   │       ├── cors.ts
│   │       ├── auth.ts
│   │       ├── validation.ts
│   │       ├── dto.ts
│   │       └── errors.ts
│   └── games/
│       └── index.ts               # thin HTTP entrypoint — routing + wiring only
└── README.md
```

### Architecture rules

1. `domain/` imports nothing outside `domain/`. Pure TS, pure functions, no `Deno`/`fetch`/`Supabase` references.
2. `application/` imports `domain/` + `ports/`. Use cases are async orchestrators.
3. `ports/` is interfaces only.
4. `infrastructure/` implements `ports/` — only place that touches Supabase or `Deno.readTextFile`.
5. `http/` does HTTP-specific concerns + error mapping. Knows about `Request`/`Response`.
6. `games/index.ts` is the composition root: wires concrete adapters to use cases.

Migrating away from Supabase = swap `infrastructure/SupabaseGameRepository.ts` and `infrastructure/supabaseClient.ts`. Domain + application untouched.

## API (Edge Function `games`)

Base URL after deploy: `https://<project-ref>.supabase.co/functions/v1/games`

| Method | Path                       | Body                  | Description                  |
| ------ | -------------------------- | --------------------- | ---------------------------- |
| POST   | `/games`                   | `CreateGameRequest?`  | Create game, return id       |
| GET    | `/games/{id}`              | —                     | Fetch game state             |
| POST   | `/games/{id}/guesses`      | `{ "guess": "haben" }`| Submit a guess               |

`CreateGameRequest`: `{ "wordLength": 5, "maxAttempts": 6 }` — both optional.

**Auth:** send `Authorization: Bearer <supabase-jwt>` to bind the game to a user.
Omit the header for anonymous play (game is accessible without auth afterwards).

**Status codes:**
- `201` on create
- `200` on get / guess
- `400` invalid input (bad word, wrong length, unsupported wordLength)
- `403` JWT user does not match game owner
- `404` unknown game id
- `409` game already finished

## Local development

```bash
# 1. Install Supabase CLI (https://supabase.com/docs/guides/cli)
brew install supabase/tap/supabase   # or scoop/npm install -g supabase

# 2. Install Deno (https://deno.com)
curl -fsSL https://deno.land/install.sh | sh

# 3. Start local stack (Postgres + Studio + Auth + Edge runtime)
cd /path/to/Wortel
supabase start

# 4. Apply migrations to local Postgres
supabase db reset                    # drops + reapplies all migrations

# 5. Serve Edge Functions locally (hot reload)
supabase functions serve games --env-file ./supabase/.env.local

# 6. Smoke test
curl -X POST http://127.0.0.1:54321/functions/v1/games \
  -H 'Content-Type: application/json' \
  -d '{"wordLength":5,"maxAttempts":6}'
```

`supabase/.env.local` example (gitignored):

```
SUPABASE_URL=http://127.0.0.1:54321
SUPABASE_ANON_KEY=<from `supabase status`>
SUPABASE_SERVICE_ROLE_KEY=<from `supabase status`>
WORTEL_CORS_ALLOWED_ORIGINS=*
```

## Run Deno tests

```bash
cd supabase/functions
deno task test       # unit tests for game-logic + wordlist
deno task check      # type-check all .ts
```

## Deploy to Supabase cloud

```bash
# 1. Create project at https://supabase.com/dashboard, copy <project-ref>
supabase login
supabase link --project-ref <project-ref>

# 2. Push schema
supabase db push

# 3. Set environment for the Edge Function
supabase secrets set WORTEL_CORS_ALLOWED_ORIGINS="*"
# SUPABASE_URL / SUPABASE_ANON_KEY / SUPABASE_SERVICE_ROLE_KEY are injected automatically.

# 4. Deploy the function
supabase functions deploy games

# 5. Verify
curl -X POST https://<project-ref>.supabase.co/functions/v1/games \
  -H 'Content-Type: application/json' \
  -d '{"wordLength":5}'
```

## Auth notes

- Email signup is enabled by default in `config.toml`. Disable confirmations for fast
  dev: already off.
- Anonymous sign-ins are enabled (`enable_anonymous_sign_ins = true`). Mobile client
  can call `auth.signInAnonymously()` to get a JWT and create owned games even
  without an account.
- For production, lock down `WORTEL_CORS_ALLOWED_ORIGINS` to the mobile app's web
  preview URL (if any) and rely on the function-level JWT check otherwise.

## Database

- `public.games` is the single table. JSONB `attempts` keeps the design Edge-Function-friendly without joins.
- RLS policies enforce ownership for authenticated rows. The Edge Function uses the
  service-role key (RLS-bypassing) and applies ownership checks in code, so anonymous
  games (`user_id IS NULL`) remain accessible without policy violations.

## Maintenance: regenerate wordlists

`wordlists/wordlist_creator.py` downloads the German FrequencyWords list, filters, and writes
`words_{5,6,7}_letters.txt`. Edge Functions need them as **TS modules** (not txt) so the bundler
ships them with the function.

After regenerating txt files, convert to ts:

```bash
cd supabase/functions/_shared/infrastructure/data
for L in 5 6 7; do
  python3 -c "
with open('words_${L}_letters.txt') as f:
    words = [w.strip() for w in f if w.strip()]
with open('words_${L}_letters.ts', 'w') as out:
    out.write('// Auto-generated. Do not edit by hand.\n\n')
    out.write('export const WORDS_${L}: readonly string[] = [\n')
    for w in words: out.write(f'  \"{w}\",\n')
    out.write('];\n')
"
done
```

Then redeploy: `supabase functions deploy games` (or push to main and let Actions run).
