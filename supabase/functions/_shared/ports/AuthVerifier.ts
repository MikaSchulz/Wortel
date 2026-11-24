/**
 * Outbound port: turns a bearer token into the authenticated user's id.
 * Implementations return null for anything that is not a valid user session
 * (expired JWT, garbage, an API key sent as bearer) — never throw for that.
 */
export interface AuthVerifier {
  userIdFromToken(token: string): Promise<string | null>;
}
