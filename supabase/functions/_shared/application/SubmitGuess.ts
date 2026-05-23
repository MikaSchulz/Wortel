import { Game } from "../domain/game/Game.ts";
import { DomainError } from "../domain/shared/DomainError.ts";
import { ConcurrencyError, GameRepository } from "../ports/GameRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";

export interface SubmitGuessInput {
  gameId: string;
  guess: string;
  userId: string | null;
}

/**
 * Use case: validate ownership, delegate guess handling to the Game aggregate,
 * persist with optimistic locking. Retries once on concurrent modification.
 */
export class SubmitGuess {
  constructor(
    private readonly games: GameRepository,
    private readonly words: WordRepository,
  ) {}

  async execute(input: SubmitGuessInput): Promise<Game> {
    return await this.run(input, /* retriesLeft */ 1);
  }

  private async run(input: SubmitGuessInput, retriesLeft: number): Promise<Game> {
    const game = await this.games.findById(input.gameId);
    if (!game) {
      throw new DomainError("GAME_NOT_FOUND", `Game ${input.gameId} not found`);
    }
    if (!game.canBeAccessedBy(input.userId)) {
      throw new DomainError("FORBIDDEN", "Not allowed to access this game");
    }

    // Validator passed as a function — Game stays decoupled from WordRepository
    // (would otherwise force the domain to know about Promises and adapters).
    // We pre-fetch validity to keep the aggregate API synchronous.
    const valid = await this.words.isValid(input.guess);
    game.submitGuess(input.guess, () => valid);

    try {
      return await this.games.save(game);
    } catch (e) {
      if (e instanceof ConcurrencyError && retriesLeft > 0) {
        return await this.run(input, retriesLeft - 1);
      }
      throw e;
    }
  }
}
