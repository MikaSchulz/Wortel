import { GameSnapshot } from "../domain/game/Game.ts";
import { GameStatus } from "../domain/game/GameStatus.ts";
import { GuessResult } from "../domain/game/Guess.ts";

/** Shape of a `public.games` row as returned by PostgREST. */
export interface GameRow {
  id: string;
  user_id: string | null;
  secret_word: string;
  word_length: number;
  max_attempts: number;
  status: GameStatus;
  attempts: GuessResult[] | null;
  version: number;
  daily_day?: string | null;
  anonymous_id?: string | null;
}

/** Columns needed to rebuild a Game — avoids shipping unrelated columns. */
export const GAME_COLUMNS =
  "id, user_id, secret_word, word_length, max_attempts, status, attempts, version";

export function rowToSnapshot(row: GameRow): GameSnapshot {
  return {
    id: row.id,
    ownerId: row.user_id,
    secretWord: row.secret_word,
    wordLength: row.word_length,
    maxAttempts: row.max_attempts,
    attempts: row.attempts ?? [],
    status: row.status,
    version: row.version,
  };
}

/** Insert payload for a brand-new game (version always starts at 0). */
export function snapshotToInsertRow(snap: GameSnapshot): Omit<GameRow, "version"> & {
  version: 0;
} {
  return {
    id: snap.id,
    user_id: snap.ownerId,
    secret_word: snap.secretWord,
    word_length: snap.wordLength,
    max_attempts: snap.maxAttempts,
    attempts: snap.attempts,
    status: snap.status,
    version: 0,
  };
}
