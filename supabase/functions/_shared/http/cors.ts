// CORS helpers + standard JSON response builders.

// Permissive header allowlist. Safe because we don't use credentialed CORS
// (no Access-Control-Allow-Credentials) — RLS + JWT validation in Edge
// Functions handle authorization. Supabase clients send several headers
// (x-region, x-client-info, x-supabase-api-version, …) plus our own
// (x-anonymous-id), so an explicit list is brittle. `*` here matches any
// header in the preflight Access-Control-Request-Headers list.
const ALLOWED_HEADERS = "*";

const ALLOWED_METHODS = "GET, POST, OPTIONS";

export function corsHeaders(origin: string | null): Record<string, string> {
  const allowedEnv = Deno.env.get("WORTEL_CORS_ALLOWED_ORIGINS") ?? "*";
  const allowList = allowedEnv.split(",").map((s) => s.trim()).filter(Boolean);

  let allowOrigin = "*";
  if (allowList.length > 0 && allowList[0] !== "*") {
    allowOrigin = origin && allowList.includes(origin) ? origin : allowList[0];
  } else if (origin) {
    allowOrigin = origin;
  }

  return {
    "Access-Control-Allow-Origin": allowOrigin,
    "Access-Control-Allow-Methods": ALLOWED_METHODS,
    "Access-Control-Allow-Headers": ALLOWED_HEADERS,
    "Access-Control-Max-Age": "3600",
    "Vary": "Origin",
  };
}

export function handlePreflight(req: Request): Response | null {
  if (req.method !== "OPTIONS") return null;
  return new Response(null, {
    status: 204,
    headers: corsHeaders(req.headers.get("origin")),
  });
}

export function jsonResponse(
  req: Request,
  body: unknown,
  init: ResponseInit = {},
): Response {
  return new Response(JSON.stringify(body), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...corsHeaders(req.headers.get("origin")),
      ...(init.headers ?? {}),
    },
  });
}

export function errorResponse(
  req: Request,
  status: number,
  message: string,
): Response {
  return jsonResponse(req, { error: message }, { status });
}
