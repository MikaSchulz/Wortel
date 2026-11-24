import { AuthVerifier } from "../ports/AuthVerifier.ts";

/**
 * Extracts the bearer token from the `Authorization` header, or null.
 * Pure HTTP parsing — verification is the AuthVerifier's job.
 */
export function bearerToken(req: Request): string | null {
  const auth = req.headers.get("Authorization");
  if (!auth) return null;
  const match = /^bearer\s+(.+)$/i.exec(auth.trim());
  return match ? match[1].trim() : null;
}

/**
 * Authenticated user id for the request. Null for anonymous calls, invalid
 * or expired tokens, and API keys sent as bearer (the mobile client sends
 * the publishable key when it has no session).
 */
export async function getUserId(
  req: Request,
  verifier: AuthVerifier,
): Promise<string | null> {
  const token = bearerToken(req);
  if (!token) return null;
  return await verifier.userIdFromToken(token);
}
