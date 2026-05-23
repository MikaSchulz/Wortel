import { LetterResult } from "./LetterResult.ts";

export interface GuessResult {
  guess: string;
  result: LetterResult[];
}
