import { userClient } from "../infrastructure/supabaseClient.ts";

/**
 * Extract the authenticated user id from the request's JWT.
 * Returns null for anonymous calls or invalid tokens.
 */
export async function getUserId(req: Request): Promise<string | null> {
  const auth = req.headers.get("Authorization");
  if (!auth || !auth.toLowerCase().startsWith("bearer ")) return null;
  try {
    const { data, error } = await userClient(req).auth.getUser();
    if (error) return null;
    return data.user?.id ?? null;
  } catch (_e) {
    return null;
  }
}
