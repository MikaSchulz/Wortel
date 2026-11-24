import { SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { AuthVerifier } from "../ports/AuthVerifier.ts";

/**
 * Verifies Supabase access tokens via `auth.getClaims()`.
 *
 * With asymmetric JWT signing keys (the Supabase default for new projects)
 * the signature is checked locally against the cached JWKS — no round trip
 * to the Auth server per request. Projects still on the legacy shared
 * secret transparently fall back to `getUser()`, i.e. the old behaviour.
 */
export class SupabaseAuthVerifier implements AuthVerifier {
  constructor(private readonly client: SupabaseClient) {}

  async userIdFromToken(token: string): Promise<string | null> {
    try {
      const { data, error } = await this.client.auth.getClaims(token);
      if (error || !data) return null;
      const sub = data.claims.sub;
      return typeof sub === "string" && sub.length > 0 ? sub : null;
    } catch {
      // Not a JWT at all (e.g. the publishable key sent as bearer).
      return null;
    }
  }
}
