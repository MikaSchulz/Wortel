Open Tasks — Single Source of Truth

Zweck
Dieses Dokument fasst alle offenen Aufgaben und den aktuellen Projektstatus zusammen.

Erledigt
- [x] Supabase-Backend (Edge Function `games`, Postgres, Auth, RLS), hexagonale Architektur
- [x] Deno-Tests, Typecheck, Lint, Format + Architektur-Test in CI
- [x] Supabase-Projekt angelegt, Deploy per GitHub Actions
- [x] Mobile-App (Kotlin/Compose Multiplatform): Android, Web (GitHub Pages), iOS-Framework
- [x] Login: Google + Gast; Spielstand übersteht App-Neustart auf allen Plattformen
- [x] Tipp-Funktion, Farbenblind-Modus
- [x] Tageschallenge im Backend (`/games/daily`, lazy erzeugt, ohne pg_cron)
- [x] Toolchain-Update 11/2026 (Kotlin 2.4, CMP 1.12, AGP 9.5, Supabase CLI 3, Actions auf Node 24)

Offen — Betrieb
- [ ] `SUPABASE_ACCESS_TOKEN` im GitHub-Secret erneuern — der Deploy vom 23.10.2026 scheiterte mit „Invalid access token“
- [ ] `[db] major_version` in `supabase/config.toml` mit der Postgres-Version des Cloud-Projekts abgleichen (Dashboard → Settings → Infrastructure)
- [ ] JWT-Signing-Keys des Projekts auf asymmetrisch umstellen (falls noch Legacy-Secret), dann prüft die Function Tokens ohne Auth-Roundtrip

Offen — Features
- [ ] Tageschallenge im Client (UI + `POST /games/daily`)
- [ ] Statistiken (Streaks, gewonnene Spiele pro User)
- [ ] iOS: Xcode-Projekt (`iosApp/`) anlegen (Mac nötig)
- [ ] App-Icon, Share-Ergebnis (Emoji-Raster), Haptik
- [ ] Optional: Wortlisten-Admin-Endpunkt + Liste in DB statt Function-Bundle
