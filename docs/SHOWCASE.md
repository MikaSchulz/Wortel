# Wortel — Live Showcase

Live Backend running on Supabase Edge Functions + Postgres + Auth.

## Project info

| Field | Value |
| --- | --- |
| Supabase Project Ref | `lmmjbpgfnuxbcdvjtdkm` |
| Base URL (Edge Functions) | `https://lmmjbpgfnuxbcdvjtdkm.supabase.co/functions/v1` |
| Publishable Key (safe for client) | `sb_publishable_NwY2X2Dj3T7O1d1-hWgOVQ_BFx1CYys` |
| Region | (set during project creation) |
| Dashboard | <https://supabase.com/dashboard/project/lmmjbpgfnuxbcdvjtdkm> |

The publishable key is **safe to embed** in browser, mobile, or share publicly — RLS policies on `public.games` enforce ownership server-side.

The secret key (`sb_secret_*`) is **never** in this repo or shipped to clients. It is injected by the Supabase runtime into Edge Functions only.

## API quick test

```bash
export PROJECT_REF="lmmjbpgfnuxbcdvjtdkm"
export PUB="sb_publishable_NwY2X2Dj3T7O1d1-hWgOVQ_BFx1CYys"
export BASE="https://${PROJECT_REF}.supabase.co/functions/v1/games"

# Create a new game (anonymous — no user binding)
GAME=$(curl -s -X POST "$BASE" \
  -H "Authorization: Bearer $PUB" \
  -H "Content-Type: application/json" \
  -d '{"wordLength":5,"maxAttempts":6}')
echo "$GAME" | jq
ID=$(echo "$GAME" | jq -r .id)

# Fetch state
curl -s "$BASE/$ID" -H "Authorization: Bearer $PUB" | jq

# Submit a guess (must be valid German word from the bundled list)
curl -s -X POST "$BASE/$ID/guesses" \
  -H "Authorization: Bearer $PUB" \
  -H "Content-Type: application/json" \
  -d '{"guess":"haben"}' | jq
```

Expected response shape:

```json
{
  "id": "uuid",
  "status": "RUNNING",
  "attempts": [
    { "guess": "haben", "result": ["CORRECT","CORRECT","CORRECT","CORRECT","CORRECT"] }
  ],
  "remainingAttempts": 5,
  "wordLength": 5,
  "maxAttempts": 6
}
```

## Endpoints

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| POST | `/games` | `{wordLength?:5\|6\|7, maxAttempts?:1..20}` | 201 — `{id, wordLength, maxAttempts}` |
| GET | `/games/{id}` | — | 200 — full game state |
| POST | `/games/{id}/guesses` | `{guess: "haben"}` | 200 — updated state |
| GET | `/games/daily` | — | 200 — today's challenge metadata |
| POST | `/games/daily` | — | 200 — daily game state (idempotent) |

### Daily Wordle

One shared secret per UTC day. Lazily generated on first request. User can play once
per day; subsequent `POST /games/daily` calls return the same in-progress game so the
UI can restore state. Anonymous play needs an `X-Anonymous-Id: <uuid>` header.

```bash
# Authenticated play
curl -s -X POST "$BASE/daily" -H "Authorization: Bearer $JWT" | jq

# Anonymous play
curl -s -X POST "$BASE/daily" \
  -H "Authorization: Bearer $PUB" \
  -H "X-Anonymous-Id: 11111111-1111-1111-1111-111111111111" | jq
```

Response shape:

```json
{
  "challenge": { "day": "2026-05-24", "wordLength": 5, "maxAttempts": 6 },
  "game": { "id": "uuid", "status": "RUNNING", "attempts": [], "remainingAttempts": 6, "wordLength": 5, "maxAttempts": 6 },
  "alreadyPlayed": false
}
```

## Auth modes

| Header | Behaviour |
| --- | --- |
| _(none)_ + publishable key as Bearer | Anonymous game — accessible by any caller afterwards |
| `Authorization: Bearer <supabase-user-jwt>` | Game bound to user; only that user can read/guess |

To get a user JWT in a client app, use the Supabase SDK:

```typescript
import { createClient } from "jsr:@supabase/supabase-js@2";
const supabase = createClient(
  "https://lmmjbpgfnuxbcdvjtdkm.supabase.co",
  "sb_publishable_NwY2X2Dj3T7O1d1-hWgOVQ_BFx1CYys",
);
const { data, error } = await supabase.auth.signInAnonymously();
// Use data.session.access_token as Bearer for /games endpoints to bind games to the anon user.
```

## Observability

- **Function logs:** Dashboard → Edge Functions → `games` → Logs
- **DB query logs:** Dashboard → Database → Logs
- **Auth events:** Dashboard → Authentication → Logs
- **Table data:** Dashboard → Table Editor → `games`

## Deploy pipeline

Every push to `main` triggers `.github/workflows/deploy.yml`:

```
verify (deno check + test + lint)
   │
   ├─▶ deploy-db (supabase db push)
   └─▶ deploy-functions (supabase functions deploy games)
                                │
                                ▼
                          smoke-test (curl POST /games)
```

Last successful deploy: commit `79b44e6` — wordlists bundled as TS modules.

## Manual rollback

```bash
git revert <bad-sha>
git push origin main
# Actions auto-deploys the reverted code
```

Or trigger workflow on previous SHA:
GitHub → Actions → "Deploy to Supabase" → Run workflow → pick ref.

## Cost & limits (Free tier)

- 500 MB DB
- 500 K Edge Function invocations / month
- 2 GB egress
- 50 K monthly active Auth users

Current usage: Dashboard → Settings → Billing → Usage.
