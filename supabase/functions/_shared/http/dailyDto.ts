import { DailyChallenge } from "../domain/daily/DailyChallenge.ts";
import { Game } from "../domain/game/Game.ts";
import { toGameStateResponse, GameStateResponse } from "./dto.ts";

export interface DailyChallengeMetaResponse {
  day: string;
  wordLength: number;
  maxAttempts: number;
}

export interface DailyPlayResponse {
  challenge: DailyChallengeMetaResponse;
  game: GameStateResponse;
  alreadyPlayed: boolean;
}

export function toMetaResponse(c: DailyChallenge): DailyChallengeMetaResponse {
  return {
    day: c.day,
    wordLength: c.wordLength,
    maxAttempts: c.maxAttempts,
  };
}

export function toDailyPlayResponse(
  challenge: DailyChallenge,
  game: Game,
  alreadyPlayed: boolean,
): DailyPlayResponse {
  return {
    challenge: toMetaResponse(challenge),
    game: toGameStateResponse(game),
    alreadyPlayed,
  };
}
