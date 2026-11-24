import { SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { Game } from "../domain/game/Game.ts";
import { ConcurrencyError, GameRepository } from "../ports/GameRepository.ts";
import { GAME_COLUMNS, GameRow, rowToSnapshot, snapshotToInsertRow } from "./gameRow.ts";

/**
 * Concrete repository backed by Supabase Postgres `public.games`.
 * Uses optimistic locking via the `version` column.
 */
export class SupabaseGameRepository implements GameRepository {
  constructor(private readonly db: SupabaseClient) {}

  async findById(id: string): Promise<Game | null> {
    const { data, error } = await this.db
      .from("games")
      .select(GAME_COLUMNS)
      .eq("id", id)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    return Game.restore(rowToSnapshot(data as GameRow));
  }

  async insert(game: Game): Promise<Game> {
    const { data, error } = await this.db
      .from("games")
      .insert(snapshotToInsertRow(game.toSnapshot()))
      .select(GAME_COLUMNS)
      .single();
    if (error) throw error;
    return Game.restore(rowToSnapshot(data as GameRow));
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
      .select(GAME_COLUMNS)
      .maybeSingle();
    if (error) throw error;
    if (!data) throw new ConcurrencyError(snap.id);
    return Game.restore(rowToSnapshot(data as GameRow));
  }
}
