import { SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { Game, GameSnapshot } from "../domain/game/Game.ts";
import { GameStatus } from "../domain/game/GameStatus.ts";
import { GuessResult } from "../domain/game/Guess.ts";
import { ConcurrencyError, GameRepository } from "../ports/GameRepository.ts";

interface GameRow {
  id: string;
  user_id: string | null;
  secret_word: string;
  word_length: number;
  max_attempts: number;
  status: GameStatus;
  attempts: GuessResult[];
  version: number;
}

/**
 * Concrete repository backed by Supabase Postgres `public.games`.
 * Uses optimistic locking via the `version` column.
 */
export class SupabaseGameRepository implements GameRepository {
  constructor(private readonly db: SupabaseClient) {}

  async findById(id: string): Promise<Game | null> {
    const { data, error } = await this.db
      .from("games")
      .select("*")
      .eq("id", id)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    return Game.restore(this.toSnapshot(data as GameRow));
  }

  async insert(game: Game): Promise<Game> {
    const snap = game.toSnapshot();
    const { data, error } = await this.db
      .from("games")
      .insert({
        id: snap.id,
        user_id: snap.ownerId,
        secret_word: snap.secretWord,
        word_length: snap.wordLength,
        max_attempts: snap.maxAttempts,
        attempts: snap.attempts,
        status: snap.status,
        version: 0,
      })
      .select("*")
      .single();
    if (error) throw error;
    return Game.restore(this.toSnapshot(data as GameRow));
  }

  async save(game: Game): Promise<Game> {
    const snap = game.toSnapshot();
    const { data, error } = await this.db
      .from("games")
      .update({
        attempts: snap.attempts,
        status: snap.status,
        version: snap.version + 1,
      })
      .eq("id", snap.id)
      .eq("version", snap.version)
      .select("*")
      .maybeSingle();
    if (error) throw error;
    if (!data) throw new ConcurrencyError(snap.id);
    return Game.restore(this.toSnapshot(data as GameRow));
  }

  private toSnapshot(row: GameRow): GameSnapshot {
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
}
