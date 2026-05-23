import { Game } from "../domain/game/Game.ts";

/**
 * Outbound port. Use cases depend on this interface; concrete adapter
 * lives in `infrastructure/SupabaseGameRepository.ts`.
 */
export interface GameRepository {
  findById(id: string): Promise<Game | null>;
  insert(game: Game): Promise<Game>;
  /**
   * Optimistic locking save. Increments version. Throws ConcurrencyError
   * if the row was modified by someone else since `game` was loaded.
   */
  save(game: Game): Promise<Game>;
}

export class ConcurrencyError extends Error {
  constructor(public readonly gameId: string) {
    super(`Concurrent modification detected for game ${gameId}`);
    this.name = "ConcurrencyError";
  }
}
