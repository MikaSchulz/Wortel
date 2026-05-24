import { Game } from "../domain/game/Game.ts";
import { GameStatus, isTerminal } from "../domain/game/GameStatus.ts";
import { GuessResult } from "../domain/game/Guess.ts";
import { GuessRejection } from "../application/SubmitGuess.ts";

export interface CreateGameResponse {
  id: string;
  wordLength: number;
  maxAttempts: number;
}

export interface GameStateResponse {
  id: string;
  status: GameStatus;
  attempts: GuessResult[];
  remainingAttempts: number;
  wordLength: number;
  maxAttempts: number;
  secretWord?: string;
  /**
   * Present iff the last submitted guess was rejected by the dictionary
   * or for length reasons. Clients shake the UI; no game state changed.
   */
  rejectedGuess?: GuessRejection;
}

export function toCreateGameResponse(game: Game): CreateGameResponse {
  return {
    id: game.id,
    wordLength: game.wordLength,
    maxAttempts: game.maxAttempts,
  };
}

export function toGameStateResponse(
  game: Game,
  rejection?: GuessRejection,
): GameStateResponse {
  return {
    id: game.id,
    status: game.status,
    attempts: [...game.attempts],
    remainingAttempts: game.remainingAttempts,
    wordLength: game.wordLength,
    maxAttempts: game.maxAttempts,
    secretWord: isTerminal(game.status) ? game.secretWord : undefined,
    rejectedGuess: rejection,
  };
}
