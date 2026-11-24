// Guards the hexagonal layering described in supabase/README.md.
// Fails the build as soon as a file imports across a forbidden boundary.

import { assertEquals } from "@std/assert";
import { dirname, fromFileUrl, join, relative } from "@std/path";

const ROOT = dirname(fromFileUrl(import.meta.url));

/** Which `_shared/` layers each layer may import from. */
const ALLOWED: Record<string, readonly string[]> = {
  domain: ["domain"],
  ports: ["domain", "ports"],
  application: ["domain", "ports", "application"],
  infrastructure: ["domain", "ports", "infrastructure"],
  http: ["domain", "ports", "application", "http"],
};

/** Layers that must stay free of runtime/platform dependencies. */
const PURE = new Set(["domain", "ports", "application"]);

const IMPORT_RE = /^\s*(?:import|export)\s[^"']*?from\s+["']([^"']+)["']/gm;

async function* tsFiles(dir: string): AsyncGenerator<string> {
  for await (const entry of Deno.readDir(dir)) {
    const path = join(dir, entry.name);
    if (entry.isDirectory) {
      if (entry.name === "data") continue; // generated word lists
      yield* tsFiles(path);
    } else if (entry.name.endsWith(".ts") && !entry.name.endsWith(".test.ts")) {
      yield path;
    }
  }
}

Deno.test("architecture: layers only import what they are allowed to", async () => {
  const violations: string[] = [];
  for (const layer of Object.keys(ALLOWED)) {
    for await (const file of tsFiles(join(ROOT, layer))) {
      const source = await Deno.readTextFile(file);
      for (const [, spec] of source.matchAll(IMPORT_RE)) {
        const rel = relative(ROOT, file);
        if (!spec.startsWith(".")) {
          if (PURE.has(layer)) violations.push(`${rel} imports external "${spec}"`);
          continue;
        }
        const target = relative(ROOT, join(dirname(file), spec)).split(/[\\/]/)[0];
        if (!ALLOWED[layer].includes(target)) {
          violations.push(`${rel} imports ${target}/ via "${spec}"`);
        }
      }
      if (PURE.has(layer) && /\bDeno\./.test(source)) {
        violations.push(`${relative(ROOT, file)} uses the Deno namespace`);
      }
    }
  }
  assertEquals(violations, []);
});
