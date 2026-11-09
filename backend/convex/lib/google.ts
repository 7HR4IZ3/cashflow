import { env } from "../_generated/server";
import { importPKCS8, SignJWT } from "jose";
import { interpretPurchase, record, ServiceError } from "./policy";
const packageName = "app.cashflow.mobile";
let cached: { value: string; expires: number } | null = null;
async function accessToken() {
  if (cached && cached.expires > Date.now() + 60000) return cached.value;
  const credentials = env.GOOGLE_SERVICE_ACCOUNT_JSON;
  if (!credentials) throw new ServiceError(503, "Purchase verification is not configured");
  const data = record(JSON.parse(credentials));
  if (typeof data.client_email !== "string" || typeof data.private_key !== "string") throw new ServiceError(503, "Purchase verification is not configured");
  const key = await importPKCS8(data.private_key, "RS256");
  const assertion = await new SignJWT({ scope: "https://www.googleapis.com/auth/androidpublisher" })
    .setProtectedHeader({ alg: "RS256" }).setIssuer(data.client_email)
    .setAudience("https://oauth2.googleapis.com/token").setIssuedAt().setExpirationTime("1h").sign(key);
  const response = await fetch("https://oauth2.googleapis.com/token", { method: "POST", signal: AbortSignal.timeout(10000),
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!response.ok) throw new ServiceError(503, "Purchase verification is unavailable");
  const result = record(await response.json());
  if (typeof result.access_token !== "string") throw new ServiceError(503, "Purchase verification is unavailable");
  cached = { value: result.access_token, expires: Date.now() + 3000000 };
  return cached.value;
}
export async function verifyPurchase(token: string) {
  const bearer = await accessToken();
  const base = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/purchases`;
  const response = await fetch(`${base}/subscriptionsv2/tokens/${encodeURIComponent(token)}`, {
    headers: { Authorization: `Bearer ${bearer}` }, signal: AbortSignal.timeout(10000),
  });
  if ([400, 404, 410].includes(response.status)) throw new ServiceError(401, "Purchase not found");
  if (!response.ok) throw new ServiceError(503, "Purchase verification is unavailable");
  const verified = interpretPurchase(await response.json());
  if (verified.acknowledgeProduct) {
    const ack = await fetch(`${base}/subscriptions/${verified.acknowledgeProduct}/tokens/${encodeURIComponent(token)}:acknowledge`, {
      method: "POST", headers: { Authorization: `Bearer ${bearer}`, "Content-Type": "application/json" },
      body: "{}", signal: AbortSignal.timeout(10000),
    });
    if (!ack.ok) throw new ServiceError(503, "Purchase acknowledgement is unavailable. Restore purchases to retry.");
  }
  return verified;
}
