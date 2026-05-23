import { LetterResult } from "./LetterResult.ts";

/**
 * Two-pass Wordle evaluation. Handles double letters per the standard rules:
 * Pass 1 marks all CORRECT positions and consumes those secret positions;
 * Pass 2 marks PRESENT only against still-unconsumed secret positions.
 *
 * Pure function — no I/O, easy to unit-test.
 */
export function evaluate(guess: string, secret: string): LetterResult[] {
  if (guess.length !== secret.length) {
    throw new Error(
      `guess length ${guess.length} does not match secret length ${secret.length}`,
    );
  }
  const n = guess.length;
  const result: LetterResult[] = new Array(n).fill("ABSENT");
  const secretChars = [...secret];
  const consumed = new Array(n).fill(false);

  for (let i = 0; i < n; i++) {
    if (guess[i] === secretChars[i]) {
      result[i] = "CORRECT";
      consumed[i] = true;
    }
  }
  for (let i = 0; i < n; i++) {
    if (result[i] === "CORRECT") continue;
    for (let j = 0; j < n; j++) {
      if (!consumed[j] && guess[i] === secretChars[j]) {
        result[i] = "PRESENT";
        consumed[j] = true;
        break;
      }
    }
  }
  return result;
}
