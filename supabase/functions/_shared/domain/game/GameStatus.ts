export type GameStatus = "RUNNING" | "WON" | "LOST";

export const GameStatus = {
  RUNNING: "RUNNING" as const,
  WON: "WON" as const,
  LOST: "LOST" as const,
};

export function isTerminal(status: GameStatus): boolean {
  return status === "WON" || status === "LOST";
}
