export interface Env {
  DB: D1Database;
  ALLOWED_ORIGINS: string;
  TURNSTILE_SECRET: string;
  ADMIN_TOKEN: string;
  HASH_SALT: string;
  AI: Ai;
  ASK_MODEL: string;
  VOICE_MODEL: string;
  KNOWLEDGE_URL: string;
}
