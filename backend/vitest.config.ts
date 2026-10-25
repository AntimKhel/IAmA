import { defineConfig } from "vitest/config";
import { cloudflareTest, readD1Migrations } from "@cloudflare/vitest-plugin";

// Tests run inside the Workers runtime (Miniflare) against a fresh local D1 per test.
export default defineConfig(async () => {
  const migrations = await readD1Migrations("./migrations");
  return {
    plugins: [
      cloudflareTest({
        wrangler: { configPath: "./wrangler.toml" },
        // Tests use a fake Workers AI binding, so never open a connection to Cloudflare.
        remoteBindings: false,
        miniflare: {
          bindings: {
            TEST_MIGRATIONS: migrations,
            TURNSTILE_SECRET: "test-turnstile-secret",
            ADMIN_TOKEN: "test-admin-token-0123456789",
            HASH_SALT: "test-salt",
            KNOWLEDGE_URL: "https://site.test/knowledge.json",
          },
        },
      }),
    ],
    test: { setupFiles: ["./test/setup.ts"] },
  };
});
