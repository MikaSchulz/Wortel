import { DomainError } from "../shared/DomainError.ts";
import { evaluate } from "./evaluate.ts";
import { GameStatus } from "./GameStatus.ts";
import { GuessResult } from "./Guess.ts";
import { LetterResult } from "./LetterResult.ts";

export const MIN_ATTEMPTS = 1;
export const MAX_ATTEMPTS = 20;

/**
 * Game aggregate. Holds all invariants about a single Wordle game.
 * Business decisions (guess valid? game ended? letter results?) live HERE,
 * not in use cases or the HTTP layer.
 */
export class Game {
  private constructor(
    public readonly id: string,
    public readonly ownerId: string | null,
    public readonly secretWord: string,
    public readonly wordLength: number,
    public readonly maxAttempts: number,
    private _attempts: GuessResult[],
    private _status: GameStatus,
    public readonly version: number,
  ) {}

  /**
   * Factory for a fresh game. Enforces invariants on input.
   */
  static create(args: {
    id: string;
    ownerId: string | null;
    secretWord: string;
    wordLength: number;
    maxAttempts: number;
  }): Game {
    if (!Number.isInteger(args.wordLength) || args.wordLength < 4) {
      throw new DomainError(
        "INVALID_WORD_LENGTH",
        `wordLength must be an integer >= 4, got ${args.wordLength}`,
      );
    }
    if (
      !Number.isInteger(args.maxAttempts) ||
      args.maxAttempts < MIN_ATTEMPTS ||
      args.maxAttempts > MAX_ATTEMPTS
    ) {
      throw new DomainError(
        "INVALID_MAX_ATTEMPTS",
        `maxAttempts must be in ${MIN_ATTEMPTS}..${MAX_ATTEMPTS}`,
      );
    }
    const normalizedSecret = args.secretWord.toLowerCase();
    if (normalizedSecret.length !== args.wordLength) {
      throw new DomainError(
        "SECRET_LENGTH_MISMATCH",
        `secretWord length ${normalizedSecret.length} does not match wordLength ${args.wordLength}`,
      );
    }
    return new Game(
      args.id,
      args.ownerId,
      normalizedSecret,
      args.wordLength,
      args.maxAttempts,
      [],
      GameStatus.RUNNING,
      0,
    );
  }

  /**
   * Rehydrate from a persistence snapshot. Does NOT validate as strictly as
   * `create()` — assumes a previous valid Game was persisted.
   */
  static restore(snapshot: GameSnapshot): Game {
    return new Game(
      snapshot.id,
      snapshot.ownerId,
      snapshot.secretWord,
      snapshot.wordLength,
      snapshot.maxAttempts,
      [...snapshot.attempts],
      snapshot.status,
      snapshot.version,
    );
  }

  get attempts(): ReadonlyArray<GuessResult> {
    return this._attempts;
  }

  get status(): GameStatus {
    return this._status;
  }

  get remainingAttempts(): number {
    return Math.max(0, this.maxAttempts - this._attempts.length);
  }

  /**
   * Records a guess and updates status. Returns the latest GuessResult.
   * `wordValidator` is an injected port — Game itself doesn't know how
   * dictionaries are stored.
   */
  submitGuess(
    guess: string,
    wordValidator: (normalized: string) => boolean,
  ): GuessResult {
    if (this._status !== GameStatus.RUNNING) {
      throw new DomainError("GAME_NOT_RUNNING", "Game is already finished");
    }
    const normalized = guess.toLowerCase();
    if (normalized.length !== this.wordLength) {
      throw new DomainError(
        "WRONG_LENGTH",
        `Guess length must be ${this.wordLength}`,
      );
    }
    if (!wordValidator(normalized)) {
      throw new DomainError("UNKNOWN_WORD", `Unknown word: ${normalized}`);
    }

    const result = evaluate(normalized, this.secretWord);
    const entry: GuessResult = { guess: normalized, result };
    this._attempts.push(entry);

    if (result.every((r) => r === LetterResult.CORRECT)) {
      this._status = GameStatus.WON;
    } else if (this._attempts.length >= this.maxAttempts) {
      this._status = GameStatus.LOST;
    }

    return entry;
  }

  /**
   * Ownership rule: anonymous game (ownerId null) is accessible to anyone;
   * owned games require matching userId. Centralised so use cases stay slim.
   */
  canBeAccessedBy(userId: string | null): boolean {
    if (this.ownerId === null) return true;
    return this.ownerId === userId;
  }

  toSnapshot(): GameSnapshot {
    return {
      id: this.id,
      ownerId: this.ownerId,
      secretWord: this.secretWord,
      wordLength: this.wordLength,
      maxAttempts: this.maxAttempts,
      attempts: [...this._attempts],
      status: this._status,
      version: this.version,
    };
  }
}

export interface GameSnapshot {
  id: string;
  ownerId: string | null;
  secretWord: string;
  wordLength: number;
  maxAttempts: number;
  attempts: GuessResult[];
  status: GameStatus;
  version: number;
}
