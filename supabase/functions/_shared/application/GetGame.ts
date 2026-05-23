import { Game } from "../domain/game/Game.ts";
import { DomainError } from "../domain/shared/DomainError.ts";
import { GameRepository } from "../ports/GameRepository.ts";

export interface GetGameInput {
  gameId: string;
  userId: string | null;
}

export class GetGame {
  constructor(private readonly games: GameRepository) {}

  async execute(input: GetGameInput): Promise<Game> {
    const game = await this.games.findById(input.gameId);
    if (!game) {
      throw new DomainError("GAME_NOT_FOUND", `Game ${input.gameId} not found`);
    }
    if (!game.canBeAccessedBy(input.userId)) {
      throw new DomainError("FORBIDDEN", "Not allowed to access this game");
    }
    return game;
  }
}
