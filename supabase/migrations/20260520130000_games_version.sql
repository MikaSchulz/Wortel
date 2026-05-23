-- Adds optimistic locking column to public.games.
-- Edge Functions perform `where id = $1 and version = $2`; updates increment version.
-- A failing update (0 rows affected) indicates concurrent modification.

alter table public.games
  add column if not exists version integer not null default 0;

create index if not exists games_version_idx on public.games(id, version);

comment on column public.games.version is 'Optimistic concurrency token; incremented on every update via Edge Function repository.';
