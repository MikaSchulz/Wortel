-- Daily Wordle support.
--
-- One shared secret word per calendar day (UTC). Generated lazily by the
-- Edge Function on first request of the day — keeps us off pg_cron and
-- compatible with the Supabase Free tier.
--
-- Each user can play today's challenge once; their attempt is just a normal
-- `games` row with `daily_day` set. Unique index enforces one-per-day.

set local search_path = public;

create table if not exists public.daily_challenges (
  day date primary key,
  secret_word text not null,
  word_length int not null check (word_length between 4 and 10),
  max_attempts int not null default 6 check (max_attempts between 1 and 20),
  created_at timestamptz not null default now()
);

alter table public.games
  add column if not exists daily_day date
    references public.daily_challenges(day);

-- One row per (user, day). Anonymous (user_id null) users can still play
-- the daily — they just lose access on app reinstall.
create unique index if not exists games_user_daily_day_idx
  on public.games(user_id, daily_day)
  where daily_day is not null and user_id is not null;

-- For anonymous players (user_id null) we can't enforce uniqueness via
-- (user_id, daily_day) — fall back to (id, daily_day) which is already
-- unique via primary key, so no extra index needed.

-- RLS: anyone can read today's challenge metadata (word length / day).
-- Inserts only via service_role from the Edge Function.
alter table public.daily_challenges enable row level security;

drop policy if exists "anyone reads daily challenges" on public.daily_challenges;
create policy "anyone reads daily challenges"
  on public.daily_challenges for select
  using (true);

comment on table public.daily_challenges is
  'One Wordle secret per calendar day. Populated lazily by the games Edge Function.';
comment on column public.games.daily_day is
  'Set when the game is the user''s entry for the daily challenge on that date.';
