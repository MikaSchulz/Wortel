-- Reword table comments set by earlier migrations so they no longer name
-- a third-party game. Comment-only change; no schema or data impact.

set local search_path = public;

comment on table public.games is
  'Wortel game state. Edge Functions read/write via service role; user JWT context enforces ownership in code.';

comment on table public.daily_challenges is
  'One secret word per calendar day. Populated lazily by the games Edge Function.';
