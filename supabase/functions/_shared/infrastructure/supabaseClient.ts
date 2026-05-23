import { createClient, SupabaseClient } from "jsr:@supabase/supabase-js@2";

// Supabase migrated key naming in late 2025:
//   anon public  -> publishable key (sb_publishable_*)
//   service_role -> secret key      (sb_secret_*)
// Edge Functions runtime still injects the legacy env names for back-compat;
// prefer the new names if present so we're forward-ready.

export function userClient(req: Request): SupabaseClient {
  const url = requireEnv("SUPABASE_URL");
  const publishable = firstDefinedEnv(
    "SUPABASE_PUBLISHABLE_KEY",
    "SUPABASE_ANON_KEY",
  );
  const auth = req.headers.get("Authorization") ?? "";
  return createClient(url, publishable, {
    global: { headers: { Authorization: auth } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

export function serviceClient(): SupabaseClient {
  const url = requireEnv("SUPABASE_URL");
  const secret = firstDefinedEnv(
    "SUPABASE_SECRET_KEY",
    "SUPABASE_SERVICE_ROLE_KEY",
  );
  return createClient(url, secret, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

function requireEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing required env: ${name}`);
  return value;
}

function firstDefinedEnv(...names: string[]): string {
  for (const name of names) {
    const value = Deno.env.get(name);
    if (value) return value;
  }
  throw new Error(`Missing required env: tried ${names.join(", ")}`);
}
