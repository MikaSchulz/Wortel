import { GameStatus } from "../domain/game/GameStatus.ts";
import { pickHint } from "../domain/game/hint.ts";
import { DomainError } from "../domain/shared/DomainError.ts";
import { GameRepository } from "../ports/GameRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";

export interface GetHintInput {
  gameId: string;
  userId: string | null;
}

export interface GetHintOutput {
  hint: string;
}

/**
 * Returns a single hint word for the given game. Enforces ownership
 * and runs the pure hint algorithm against the bundled word list.
 *
 * The hint must reveal a new correct-position letter; we never give
 * away the full solution, even when the player has very few options
 * left. If no qualifying word exists, throws NO_HINT_AVAILABLE so the
 * UI can show a friendly message instead of silently doing nothing.
 */
export class GetHint {
  constructor(
    private readonly games: GameRepository,
    private readonly words: WordRepository,
  ) {}

  async execute(input: GetHintInput): Promise<GetHintOutput> {
    const game = await this.games.findById(input.gameId);
    if (!game) {
      throw new DomainError("GAME_NOT_FOUND", `Game ${input.gameId} not found`);
    }
    if (!game.canBeAccessedBy(input.userId)) {
      throw new DomainError("FORBIDDEN", "Not allowed to access this game");
    }
    if (game.status !== GameStatus.RUNNING) {
      throw new DomainError("GAME_NOT_RUNNING", "Game is already finished");
    }

    const pool = await this.words.wordsForLength(game.wordLength);
    const hint = pickHint(game.secretWord, game.attempts, pool);
    if (!hint) {
      throw new DomainError(
        "NO_HINT_AVAILABLE",
        "No hint word available — try guessing the secret directly",
      );
    }
    return { hint };
  }
}
