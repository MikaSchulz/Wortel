# Deployment

Three deployment paths. Production setup uses **GitHub Actions** (`.github/workflows/deploy.yml`).

---

## Path A — GitHub Actions (Production)

`.github/workflows/deploy.yml` runs on push to `main`:

1. **verify** — `deno check`, `deno test`, `deno lint`
2. **deploy-db** — `supabase db push` applies migrations
3. **deploy-functions** — `supabase functions deploy games` + sets function secrets
4. **smoke-test** — `curl POST /games` against deployed URL

Sequential gates: if verify fails, nothing deploys.

### One-time setup

#### 1. Create Supabase project

- Go to https://supabase.com/dashboard → New Project
- Region: `eu-central-1` (Frankfurt) for DACH latency
- Save the database password — needed for `SUPABASE_DB_PASSWORD`
- Note the project ref (URL is `https://<project-ref>.supabase.co`)

#### 2. Generate access token

- Dashboard → Account → Access Tokens → Generate new token
- Scope: full access
- Copy once — `SUPABASE_ACCESS_TOKEN`

#### 3. Add repo secrets

GitHub repo → Settings → Secrets and variables → Actions → New repository secret:

| Secret | Where to find | Required |
| --- | --- | --- |
| `SUPABASE_ACCESS_TOKEN` | Account → Access Tokens (step 2) | yes |
| `SUPABASE_PROJECT_REF` | Project URL slug (e.g. `abcdefgh`) | yes |
| `SUPABASE_DB_PASSWORD` | Project creation password | yes |
| `SUPABASE_PUBLISHABLE_KEY` | Project → Settings → API Keys → Publishable key (`sb_publishable_...`) | yes (smoke test) |
| `WORTEL_CORS_ALLOWED_ORIGINS` | `*` for dev, exact mobile origin for prod | optional |

### Key naming (Supabase migration, late 2025)

Supabase renamed API keys. Old → new:

| Legacy | New | Where used |
| --- | --- | --- |
| `anon public` | **Publishable key** (`sb_publishable_*`) | Mobile client, browser, smoke test |
| `service_role` | **Secret key** (`sb_secret_*`) | Server-side, Edge Functions (RLS-bypass) |

Edge Functions runtime still injects the legacy env vars (`SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`) for backward compat. Code prefers new names (`SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY`) when present — see `supabase/functions/_shared/infrastructure/supabaseClient.ts`.

Publishable key = safe for client (RLS protects data).
Secret key = NEVER ship to client. Only in server/CI secrets.

#### 4. Add GitHub environment (optional, recommended)

Settings → Environments → New environment `production`:

- Required reviewers (manual approval before deploy)
- Wait timer (delay deploy a few minutes)
- Environment-scoped secrets (more secure than repo-level)

If you add the `production` environment, the deploy jobs auto-pause until approved.

#### 5. First deploy

Push to `main` (or run `workflow_dispatch` manually). Pipeline:

```
verify ─▶ deploy-db ─┐
        └─▶ deploy-functions ─▶ smoke-test
```

---

## Path B — Supabase native GitHub Integration

Less control, less code. Currently Beta.

1. Supabase dashboard → Settings → Integrations → GitHub → **Connect**
2. Select your repo + branch
3. Supabase auto-detects `supabase/migrations/` and `supabase/functions/`
4. On push: migrations + functions sync automatically
5. PRs get preview branches (if Branching enabled, Pro plan)

Trade-offs:
- No custom test/lint gate before deploy
- No smoke test
- No manual approval
- Faster to set up — single dashboard click

Use if: hobby project, you trust pre-deploy testing happens locally.

---

## Path C — Manual CLI

For emergency hotfixes or initial bootstrap:

```bash
supabase login
supabase link --project-ref <project-ref>
supabase db push
supabase functions deploy games
supabase secrets set WORTEL_CORS_ALLOWED_ORIGINS="*"
```

---

## PR Previews (Supabase Branching)

`.github/workflows/pr-preview.yml` is **disabled by default** (requires Pro plan).

To enable:
1. Dashboard → Settings → Branching → enable
2. Edit `pr-preview.yml`, replace `if: false` with `if: github.event.pull_request.merged != true`
3. Push — each PR now gets `pr-<number>` branch with isolated DB + function URL

Closed PR triggers cleanup automatically.

---

## Rollback

**Migrations:** Postgres migrations are forward-only by convention. To roll back: write a new migration that reverts. Never edit a deployed migration file.

**Functions:** Re-deploy previous git commit:

```bash
git checkout <previous-sha>
supabase functions deploy games --project-ref <ref>
git checkout main
```

Or trigger the deploy workflow against the old SHA via `workflow_dispatch` → ref.

---

## Observability

- Function logs: Dashboard → Edge Functions → `games` → Logs (24h retention free, 7d on Pro)
- DB logs: Dashboard → Database → Logs
- Errors: Function returns 500 with message; check logs for stack traces

Future: ship logs to external sink (Logflare, Better Stack) via Supabase log drains (Pro plan).

---

## Cost monitoring

Free tier limits (check at Dashboard → Usage):
- 500MB DB
- 500K Edge Function invocations / month
- 2GB egress
- 50,000 monthly active users (Auth)

Set up usage alerts: Settings → Billing → Usage Alerts. Recommended thresholds: 80% on each metric.

---

## Branching strategy (recommended)

```
main           ← production (auto-deploy via Actions)
  ↑
feature/xyz    ← work, PR back to main (preview env if Pro)
```

Skip `develop` / `staging` branches — Supabase preview environments make them redundant. Keep `main` always deployable.
