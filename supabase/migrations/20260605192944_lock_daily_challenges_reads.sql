-- Security hardening: keep the daily secret out of PostgREST.
--
-- The previous "anyone reads daily challenges" policy allowed anon and
-- authenticated callers to SELECT the entire row via the auto-generated
-- REST endpoint, including `secret_word`. RLS is row-level only, so a
-- broad SELECT policy leaks every column on every row it matches.
--
-- Anything the client legitimately needs (day, word_length, max_attempts)
-- is served by the Edge Function `GET /games/daily`, which goes through
-- the service-role client and bypasses RLS. So we can deny direct reads
-- altogether without breaking gameplay.

set local search_path = public;

drop policy if exists "anyone reads daily challenges" on public.daily_challenges;

-- Explicit "no direct reads" policy. We could also leave RLS on with no
-- policies (default-deny), but an explicit policy documents intent and
-- survives future "I'll just add one quick policy" temptations without
-- accidentally re-opening the door.
drop policy if exists "no direct reads" on public.daily_challenges;
create policy "no direct reads"
  on public.daily_challenges for select
  using (false);

comment on policy "no direct reads" on public.daily_challenges is
  'PostgREST callers cannot read this table — the secret_word column would leak. The games Edge Function uses the service-role client and bypasses RLS to fetch the daily metadata it actually needs.';
