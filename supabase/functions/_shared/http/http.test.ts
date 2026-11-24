// HTTP adapter tests — no network, no Supabase.

import { assertEquals } from "@std/assert";
import { DomainError } from "../domain/shared/DomainError.ts";
import { AuthVerifier } from "../ports/AuthVerifier.ts";
import { ConcurrencyError } from "../ports/GameRepository.ts";
import { bearerToken, getUserId } from "./auth.ts";
import { mapErrorToResponse } from "./errors.ts";

function req(headers: Record<string, string> = {}): Request {
  return new Request("http://localhost/functions/v1/games", { headers });
}

Deno.test("bearerToken - parses case-insensitive Bearer scheme", () => {
  assertEquals(bearerToken(req({ Authorization: "Bearer abc.def" })), "abc.def");
  assertEquals(bearerToken(req({ Authorization: "bearer   xyz " })), "xyz");
});

Deno.test("bearerToken - null without header or with other schemes", () => {
  assertEquals(bearerToken(req()), null);
  assertEquals(bearerToken(req({ Authorization: "Basic Zm9vOmJhcg==" })), null);
  assertEquals(bearerToken(req({ Authorization: "Bearer" })), null);
});

Deno.test("getUserId - delegates the token to the verifier", async () => {
  const seen: string[] = [];
  const verifier: AuthVerifier = {
    userIdFromToken(token) {
      seen.push(token);
      return Promise.resolve(token === "good" ? "user-1" : null);
    },
  };
  assertEquals(await getUserId(req({ Authorization: "Bearer good" }), verifier), "user-1");
  assertEquals(await getUserId(req({ Authorization: "Bearer bad" }), verifier), null);
  assertEquals(await getUserId(req(), verifier), null);
  assertEquals(seen, ["good", "bad"]);
});

Deno.test("mapErrorToResponse - domain errors keep code + status", async () => {
  const res = mapErrorToResponse(req(), new DomainError("GAME_NOT_FOUND", "nope"));
  assertEquals(res.status, 404);
  assertEquals(await res.json(), { error: "nope", code: "GAME_NOT_FOUND" });
});

Deno.test("mapErrorToResponse - concurrency is a 409", async () => {
  const res = mapErrorToResponse(req(), new ConcurrencyError("g1"));
  assertEquals(res.status, 409);
  assertEquals((await res.json()).code, "CONCURRENCY");
});

Deno.test("mapErrorToResponse - unexpected errors do not leak details", async () => {
  const originalError = console.error;
  console.error = () => {};
  try {
    const dbError = new Error('duplicate key value violates unique constraint "games_pkey"');
    const res = mapErrorToResponse(req(), dbError);
    assertEquals(res.status, 500);
    assertEquals(await res.json(), { error: "Internal error", code: "INTERNAL" });
  } finally {
    console.error = originalError;
  }
});
