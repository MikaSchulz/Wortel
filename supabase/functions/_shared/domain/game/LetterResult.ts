export type LetterResult = "CORRECT" | "PRESENT" | "ABSENT";

export const LetterResult = {
  CORRECT: "CORRECT" as const,
  PRESENT: "PRESENT" as const,
  ABSENT: "ABSENT" as const,
};
