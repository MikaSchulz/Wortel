import { DailyChallenge } from "../domain/daily/DailyChallenge.ts";
import { Game } from "../domain/game/Game.ts";

/**
 * Outbound port for the daily challenge feature.
 *
 * Implementations must:
 *  - Treat `findOrCreate(day)` as idempotent (concurrent calls race-safe).
 *  - Treat `findUserGame(day, userId)` and `findAnonymousGame(day, gameId)` as
 *    pure lookups (return null if not present).
 *  - Treat `insertUserGame` / `insertAnonymousGame` as serialised by the
 *    DB-level unique index — surfacing duplicates as caller-recoverable.
 */
export interface DailyChallengeRepository {
  /** Lookup today's (or any day's) challenge; create if it doesn't exist. */
  findOrCreate(
    day: string,
    pickSecret: (wordLength: number) => Promise<string>,
  ): Promise<DailyChallenge>;

  /** Returns the user's existing daily game for the day, or null. */
  findUserGame(day: string, userId: string): Promise<Game | null>;

  /**
   * Returns the anonymous player's daily game for the day, identified by a
   * client-provided cookie/UUID stored on first play. Null if none.
   */
  findAnonymousGame(day: string, anonymousId: string): Promise<Game | null>;

  /**
   * Create a new game bound to (day, userId). May throw a `DuplicateDailyEntry`
   * if a concurrent request beat us — caller should retry the lookup.
   */
  insertDailyGame(game: Game, day: string): Promise<Game>;
}

export class DuplicateDailyEntry extends Error {
  constructor(public readonly day: string, public readonly userId: string | null) {
    super(`Duplicate daily entry for ${day} / ${userId ?? "anonymous"}`);
    this.name = "DuplicateDailyEntry";
  }
}
