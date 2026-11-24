import { DailyChallenge } from "../domain/daily/DailyChallenge.ts";
import { Game } from "../domain/game/Game.ts";

/**
 * Outbound port for the daily challenge feature.
 *
 * Implementations must:
 *  - Treat `findOrCreate(day, create)` as idempotent and race-safe: when two
 *    requests create the same day concurrently, both must get back the ONE
 *    challenge that was actually stored (first writer wins, never overwrite).
 *  - Treat `findUserGame` / `findAnonymousGame` as pure lookups (null if absent).
 *  - Serialise `insertDailyGame` through a DB-level unique constraint and
 *    surface duplicates as `DuplicateDailyEntry` so the caller can re-read.
 */
export interface DailyChallengeRepository {
  /**
   * Returns the stored challenge for `day`. If none exists yet, persists the
   * one produced by `create()` — unless a concurrent request stored one first,
   * in which case that one is returned instead.
   */
  findOrCreate(
    day: string,
    create: () => Promise<DailyChallenge>,
  ): Promise<DailyChallenge>;

  /** Returns the signed-in user's daily game for the day, or null. */
  findUserGame(day: string, userId: string): Promise<Game | null>;

  /**
   * Returns the anonymous player's daily game for the day, identified by the
   * stable per-install id the client sends in `X-Anonymous-Id`. Null if none.
   */
  findAnonymousGame(day: string, anonymousId: string): Promise<Game | null>;

  /**
   * Persists a new daily game. `anonymousId` must be set exactly when
   * `game.ownerId` is null. May throw `DuplicateDailyEntry` if a concurrent
   * request beat us — the caller should re-run the lookup.
   */
  insertDailyGame(
    game: Game,
    day: string,
    anonymousId: string | null,
  ): Promise<Game>;
}

export class DuplicateDailyEntry extends Error {
  constructor(public readonly day: string, public readonly userId: string | null) {
    super(`Duplicate daily entry for ${day} / ${userId ?? "anonymous"}`);
    this.name = "DuplicateDailyEntry";
  }
}
