import { describe, expect, it, vi } from "vitest";
import { toBase64, VOICE_LIMITS } from "../src/voice";
import { call } from "./helpers";

const audio = (bytes = 4000) => new Uint8Array(bytes).map((_, i) => i % 251);

function fakeWhisper(out: unknown | (() => never)) {
  const calls: Array<{ model: string; inputs: any }> = [];
  return {
    calls,
    AI: {
      run: vi.fn(async (model: string, inputs: any) => {
        calls.push({ model, inputs });
        if (typeof out === "function") (out as () => never)();
        return out;
      }),
    },
  };
}

const send = (body: Uint8Array, o: { type?: string; ai?: unknown; ip?: string; origin?: string } = {}) =>
  call("/transcribe", { method: "POST", body, headers: { "Content-Type": o.type ?? "audio/webm" }, ai: o.ai, ip: o.ip, origin: o.origin });

describe("POST /transcribe", () => {
  it("encodes the audio for Whisper and returns clean text", async () => {
    const { AI, calls } = fakeWhisper({ text: "  Has he led teams? \n" });
    const res = await send(audio(), { ai: AI });
    expect(await res.json()).toEqual({ text: "Has he led teams?" });
    expect(calls[0].model).toBe("@cf/openai/whisper-large-v3-turbo");
    expect(calls[0].inputs.audio).toBe(toBase64(audio().buffer));
    expect(calls[0].inputs.initial_prompt).toContain("JioAirSync");
  });

  it("reads either output shape", async () => {
    const { AI } = fakeWhisper({ transcription_info: { text: "What is JioAirSync?" } });
    expect(await (await send(audio(), { ai: AI })).json()).toEqual({ text: "What is JioAirSync?" });
  });

  it("encodes large clips without blowing the stack", () => {
    const big = new Uint8Array(300_000).map((_, i) => i % 256);
    expect(atob(toBase64(big.buffer)).length).toBe(300_000);
  });

  it("rejects what isn't a short question from the site", async () => {
    const { AI } = fakeWhisper({ text: "x" });
    expect((await send(audio(), { ai: AI, type: "text/plain" })).status).toBe(415);
    expect((await send(audio(10), { ai: AI })).status).toBe(400);
    expect((await send(audio(VOICE_LIMITS.maxBytes + 1), { ai: AI })).status).toBe(413);
    expect((await send(audio(), { ai: AI, origin: "https://evil.example" })).status).toBe(403);
  });

  it("limits voice questions per visitor and handles Whisper errors", async () => {
    const { AI } = fakeWhisper({ text: "hi" });
    for (let i = 0; i < VOICE_LIMITS.perVisitorPerDay; i++) await send(audio(), { ai: AI });
    expect((await send(audio(), { ai: AI })).status).toBe(429);
    const down = fakeWhisper(() => { throw new Error("allocation exceeded"); });
    expect((await send(audio(), { ai: down.AI, ip: "198.51.100.5" })).status).toBe(503);
  });
});
