import { Game } from "../domain/game/Game.ts";
import { DomainError } from "../domain/shared/DomainError.ts";
import { GameRepository } from "../ports/GameRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";

export const DEFAULT_WORD_LENGTH = 5;
export const DEFAULT_MAX_ATTEMPTS = 6;

export interface CreateGameInput {
  wordLength?: number;
  maxAttempts?: number;
  ownerId: string | null;
}

/**
 * Use case: create a new game with a randomly chosen secret word.
 * Thin orchestrator — validation lives in `Game.create()`, randomness in `WordRepository`.
 */
export class CreateGame {
  constructor(
    private readonly games: GameRepository,
    private readonly words: WordRepository,
  ) {}

  async execute(input: CreateGameInput): Promise<Game> {
    const wordLength = input.wordLength ?? DEFAULT_WORD_LENGTH;
    const maxAttempts = input.maxAttempts ?? DEFAULT_MAX_ATTEMPTS;

    // Fail fast on obviously bad input before we touch the word list;
    // Game.create() re-checks as the authoritative invariant.
    Game.assertValidMaxAttempts(maxAttempts);

    const supported = await this.words.supportedLengths();
    if (!supported.has(wordLength)) {
      throw new DomainError(
        "INVALID_WORD_LENGTH",
        `Unsupported wordLength: ${wordLength}`,
      );
    }

    const secret = await this.words.randomWord(wordLength);
    const game = Game.create({
      id: crypto.randomUUID(),
      ownerId: input.ownerId,
      secretWord: secret,
      wordLength,
      maxAttempts,
    });
    return await this.games.insert(game);
  }
}
