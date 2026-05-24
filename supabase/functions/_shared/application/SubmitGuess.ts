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
 * A guess that the game refused for normal-gameplay reasons (unknown word,
 * wrong length). Surfaced as data instead of an exception so the HTTP layer
 * can return 200 — the user typed something the dictionary doesn't know,
 * which is not a server error.
 */
export interface GuessRejection {
  code: "UNKNOWN_WORD" | "WRONG_LENGTH";
  message: string;
}

export interface SubmitGuessResult {
  game: Game;
  rejection?: GuessRejection;
}

const REJECTION_CODES: ReadonlySet<string> = new Set(["UNKNOWN_WORD", "WRONG_LENGTH"]);

/**
 * Use case: validate ownership, delegate guess handling to the Game aggregate,
 * persist with optimistic locking. Retries once on concurrent modification.
 *
 * Word-level rejections (UNKNOWN_WORD / WRONG_LENGTH) are returned as data so
 * the HTTP layer responds 200; structural errors (forbidden, not found,
 * game ended) still throw and become 4xx.
 */
export class SubmitGuess {
  constructor(
    private readonly games: GameRepository,
    private readonly words: WordRepository,
  ) {}

  async execute(input: SubmitGuessInput): Promise<SubmitGuessResult> {
    return await this.run(input, /* retriesLeft */ 1);
  }

  private async run(input: SubmitGuessInput, retriesLeft: number): Promise<SubmitGuessResult> {
    const game = await this.games.findById(input.gameId);
    if (!game) {
      throw new DomainError("GAME_NOT_FOUND", `Game ${input.gameId} not found`);
    }
    if (!game.canBeAccessedBy(input.userId)) {
      throw new DomainError("FORBIDDEN", "Not allowed to access this game");
    }

    const valid = await this.words.isValid(input.guess);

    try {
      game.submitGuess(input.guess, () => valid);
    } catch (e) {
      if (e instanceof DomainError && REJECTION_CODES.has(e.code)) {
        return {
          game,
          rejection: { code: e.code as GuessRejection["code"], message: e.message },
        };
      }
      throw e;
    }

    try {
      const saved = await this.games.save(game);
      return { game: saved };
    } catch (e) {
      if (e instanceof ConcurrencyError && retriesLeft > 0) {
        return await this.run(input, retriesLeft - 1);
      }
      throw e;
    }
  }
}
