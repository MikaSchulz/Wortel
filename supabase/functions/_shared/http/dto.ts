import { Game } from "../domain/game/Game.ts";
import { GameStatus, isTerminal } from "../domain/game/GameStatus.ts";
import { GuessResult } from "../domain/game/Guess.ts";

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
}

export function toCreateGameResponse(game: Game): CreateGameResponse {
  return {
    id: game.id,
    wordLength: game.wordLength,
    maxAttempts: game.maxAttempts,
  };
}

export function toGameStateResponse(game: Game): GameStateResponse {
  return {
    id: game.id,
    status: game.status,
    attempts: [...game.attempts],
    remainingAttempts: game.remainingAttempts,
    wordLength: game.wordLength,
    maxAttempts: game.maxAttempts,
    secretWord: isTerminal(game.status) ? game.secretWord : undefined,
  };
}
