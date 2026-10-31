/**
 * Voice for Ask: the browser records a spoken question and posts the audio here; Whisper on Workers AI turns
 * it into text. Audio is never stored. About 47 neurons per minute of audio, so a question costs a few.
 */
import type { Env } from "./env";
import { takeQuota } from "./quota";
import { cleanLine, isAllowedOrigin, json, visitorHash } from "./util";

export const VOICE_LIMITS = {
  minBytes: 1000,
  maxBytes: 1_500_000, // ~60 s of Opus
  perVisitorPerDay: 25,
  perDay: 150,
} as const;

// Names Whisper would otherwise mishear.
const VOCABULARY =
  "Ritwik, Ritwik Raj Srivastava, MyJio, JioAirSync, JioFinance, JioMart, Jio, Reliance, HSBC, R8, ProGuard, " +
  "Kotlin, Jetpack Compose, React Native, Skoch award, Kobweb, agentic AI.";

export function toBase64(buf: ArrayBuffer): string {
  const bytes = new Uint8Array(buf);
  let binary = "";
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}

/** POST /transcribe (body: the recorded audio, e.g. audio/webm or audio/mp4) → { text } */
export async function transcribe(request: Request, env: Env): Promise<Response> {
  if (!isAllowedOrigin(request.headers.get("Origin"), env)) return json({ error: "Forbidden" }, 403);
  if (!/^audio\//i.test(request.headers.get("Content-Type") ?? "")) return json({ error: "Expected audio." }, 415);
  const audio = await request.arrayBuffer();
  if (audio.byteLength < VOICE_LIMITS.minBytes) return json({ error: "Didn't catch that. Try again?" }, 400);
  if (audio.byteLength > VOICE_LIMITS.maxBytes) return json({ error: "That's a long one. Keep questions under a minute." }, 413);

  const quota = await takeQuota(env, "voice", await visitorHash(request, env), VOICE_LIMITS);
  if (!quota.ok) return json({ error: "Voice is resting for today. You can still type your question." }, quota.reason === "visitor" ? 429 : 503);

  try {
    const out = (await env.AI.run(env.VOICE_MODEL as keyof AiModels, {
      audio: toBase64(audio),
      task: "transcribe",
      language: "en",
      vad_filter: true,
      condition_on_previous_text: false,
      initial_prompt: VOCABULARY,
    } as never)) as { text?: unknown; transcription_info?: { text?: unknown } };
    const text = cleanLine(out?.text ?? out?.transcription_info?.text).slice(0, 300);
    return json({ text }, 200, { "Cache-Control": "no-store" });
  } catch (err) {
    console.error("voice", err);
    return json({ error: "Voice is resting for today. You can still type your question." }, 503);
  }
}
