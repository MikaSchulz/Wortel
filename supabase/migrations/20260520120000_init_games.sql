-- Wortel: initial schema for games + RLS.
-- Applied via `supabase db push` or auto-run by Supabase platform on link/deploy.

set local search_path = public;

create extension if not exists "pgcrypto";

-- Game status as Postgres enum (matches LetterResult/GameStatus on the TS side).
do $$
begin
  if not exists (select 1 from pg_type where typname = 'game_status') then
    create type game_status as enum ('RUNNING', 'WON', 'LOST');
  end if;
end$$;

create table if not exists public.games (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade,
  secret_word text not null,
  word_length int not null check (word_length between 4 and 10),
  max_attempts int not null check (max_attempts between 1 and 20),
  status game_status not null default 'RUNNING',
  attempts jsonb not null default '[]'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists games_user_id_idx on public.games(user_id);
create index if not exists games_status_idx on public.games(status);

-- updated_at trigger
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists set_updated_at on public.games;
create trigger set_updated_at
  before update on public.games
  for each row
  execute function public.set_updated_at();

-- RLS: each user can only see and modify their own games.
-- Anonymous games (user_id null) are only manageable via service_role inside Edge Functions.
alter table public.games enable row level security;

drop policy if exists "users select own games" on public.games;
create policy "users select own games"
  on public.games for select
  using (auth.uid() is not null and user_id = auth.uid());

drop policy if exists "users insert own games" on public.games;
create policy "users insert own games"
  on public.games for insert
  with check (auth.uid() is not null and user_id = auth.uid());

drop policy if exists "users update own games" on public.games;
create policy "users update own games"
  on public.games for update
  using (auth.uid() is not null and user_id = auth.uid())
  with check (auth.uid() is not null and user_id = auth.uid());

drop policy if exists "users delete own games" on public.games;
create policy "users delete own games"
  on public.games for delete
  using (auth.uid() is not null and user_id = auth.uid());

-- service_role bypasses RLS automatically, so Edge Functions using SUPABASE_SERVICE_ROLE_KEY
-- can manage anonymous games (user_id IS NULL).

comment on table public.games is 'Wordle game state. Edge Functions read/write via service role; user JWT context enforces ownership in code.';
