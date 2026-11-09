import { env } from "./_generated/server";
import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";
import { internal, components } from "./_generated/api";
import { RateLimiter, MINUTE } from "@convex-dev/rate-limiter";
import { hash, limits, record, ServiceError, text } from "./lib/policy";
import { verifyPurchase } from "./lib/google";
import { askJev, type Suggestion } from "./lib/jev";

const limiter = new RateLimiter(components.rateLimiter, {
  global: { kind: "token bucket", rate: 600, period: MINUTE, capacity: 100 },
  token: { kind: "token bucket", rate: 20, period: MINUTE, capacity: 10 },
});
const http = httpRouter();
const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });
for (const path of ["/v1/subscription", "/v1/analyse"]) {
  http.route({ path, method: "POST", handler: httpAction(async (ctx, request) => {
    try {
      const authorization = request.headers.get("Authorization") ?? "";
      if (!authorization.startsWith("Bearer ")) throw new ServiceError(401, "Purchase required");
      const token = text(authorization.slice(7), 4096);
      const tokenHash = await hash(token);
      if (!(await limiter.limit(ctx, "global")).ok || !(await limiter.limit(ctx, "token", { key: tokenHash })).ok) throw new ServiceError(429, "Please wait before trying again");
      // Read the stream with a hard byte cap; Content-Length alone is not trusted.
      const reader = request.body?.getReader();
      let length = 0;
      const chunks: Uint8Array[] = [];
      if (reader) while (true) {
        const { value, done } = await reader.read(); if (done) break;
        length += value.byteLength;
        if (length > 16384) { await reader.cancel(); throw new ServiceError(413, "Alert is too long"); }
        chunks.push(value);
      }
      const bytes = new Uint8Array(length); let offset = 0;
      for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
      let body: Record<string, unknown>;
      try { body = record(JSON.parse(new TextDecoder().decode(bytes) || "{}")); } catch { throw new ServiceError(400, "Invalid request"); }
      // Validate consent and bounds before any provider request. Raw text stays in this HTTP action only.
      const analysis = path === "/v1/analyse";
      if (analysis && body.consent !== true) throw new ServiceError(400, "AI consent required");
      const alert = analysis ? text(body.text, 2000) : "";
      const source = analysis ? text(body.source, 200) : "";
      const requestId = analysis ? text(body.requestId, 512) : "";
      if (analysis && /\b(otp|one[- ]time|verification code|security code|passcode)\b/i.test(alert)) throw new ServiceError(400, "Authentication alerts cannot be sent to AI");
      const verified = await verifyPurchase(token);
      if (verified.tier === "FREE") {
        if (analysis) throw new ServiceError(403, "An active Lite or Premium subscription is required");
        // A pending replacement must not revoke the currently active purchase.
        return reply({ tier: "FREE", expiresAt: 0, month: new Date().toISOString().slice(0, 7), used: 0 });
      }
      const root: string = await ctx.runMutation(internal.billing.link, {
        tokenHash,
        accountHash: verified.account ? await hash(`account:${verified.account}`) : null,
        linkedHash: verified.linkedToken ? await hash(verified.linkedToken) : null,
      });
      const month = new Date().toISOString().slice(0, 7);
      const snapshot = async () => ({ tier: verified.tier, expiresAt: verified.expiresAt, month,
        used: await ctx.runQuery(internal.billing.usage, { root, month }) });
      if (!analysis) return reply(await snapshot());
      if (!env.TYPESAFE_API_KEY) throw new ServiceError(503, "AI help is not configured");
      const lease = crypto.randomUUID();
      const reservation = await ctx.runMutation(internal.billing.reserve, { root, month, requestId, fingerprint: await hash(`${source}\n${alert}`), tier: verified.tier, lease });
      if (reservation.status === "limit") throw new ServiceError(429, `Monthly allowance of ${limits[verified.tier]} reached`);
      if (reservation.status === "busy") throw new ServiceError(409, "This alert is already being checked");
      if (reservation.status === "conflict") throw new ServiceError(409, "This request ID belongs to a different alert");
      let suggestion: Suggestion | null = reservation.result;
      if (reservation.status === "reserved") {
        try {
          suggestion = await askJev(alert, source);
          await ctx.runMutation(internal.billing.finish, { root, requestId, lease, result: suggestion });
        } catch (error) {
          await ctx.runMutation(internal.billing.finish, { root, requestId, lease, result: null });
          throw error;
        }
      }
      return reply({ suggestion, subscription: await snapshot() });
    } catch (error) {
      // Never return or log provider response bodies, purchase tokens, or alert text.
      return reply({ error: error instanceof ServiceError ? error.message : "Service unavailable. Please try again.", code: error instanceof ServiceError && error.message.startsWith("Monthly allowance") ? "quota_exhausted" : "service_error" }, error instanceof ServiceError ? error.status : 503);
    }
  }) });
}
http.route({ path: "/health", method: "GET", handler: httpAction(async () => reply({ ok: true, billingReady: env.ENABLE_PURCHASES === "true" && Boolean(env.GOOGLE_SERVICE_ACCOUNT_JSON) && Boolean(env.TYPESAFE_API_KEY), aiReady: Boolean(env.TYPESAFE_API_KEY) })) });
export default http;
