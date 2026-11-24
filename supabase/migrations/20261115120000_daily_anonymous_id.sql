-- Anonymous daily players get their own column instead of borrowing games.id.
--
-- Until now the client-supplied X-Anonymous-Id was used as the primary key
-- of the player's daily game. Because that id is stable per install, the
-- player's second daily (next day) collided with their own first row on
-- games_pkey and the request failed. The id now lives in `anonymous_id`,
-- and every daily game gets a fresh primary key.

set local search_path = public;

alter table public.games
  add column if not exists anonymous_id uuid;

-- Existing anonymous daily rows were keyed by id = anonymous id. Backfill so
-- players who already started today's daily get their game back.
update public.games
   set anonymous_id = id
 where daily_day is not null
   and user_id is null
   and anonymous_id is null;

-- One daily entry per (anonymous player, day) — mirrors
-- games_user_daily_day_idx for signed-in users.
create unique index if not exists games_anon_daily_day_idx
  on public.games(anonymous_id, daily_day)
  where daily_day is not null and anonymous_id is not null;

comment on column public.games.anonymous_id is
  'Client-generated per-install id (X-Anonymous-Id) of an anonymous daily player. Null for signed-in players and freestyle games.';
