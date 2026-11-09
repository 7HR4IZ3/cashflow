export type Tier = "FREE" | "LITE" | "PREMIUM";
export const limits = { FREE: 0, LITE: 300, PREMIUM: 3000 } as const;
export const products: Record<string, Tier> = { cashflow_lite: "LITE", cashflow_premium: "PREMIUM" };
export class ServiceError extends Error {
  constructor(public status: number, message: string) { super(message); }
}
export function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new ServiceError(400, "Invalid request");
  return value as Record<string, unknown>;
}
export function text(value: unknown, max: number): string {
  if (typeof value !== "string" || !value.trim() || value.length > max) throw new ServiceError(400, "Invalid request");
  return value;
}
export async function hash(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
}
export interface VerifiedPurchase { tier: Tier; expiresAt: number; account: string | null; linkedToken: string | null; acknowledgeProduct: string | null }
export function interpretPurchase(raw: unknown, now = Date.now()): VerifiedPurchase {
  const body = record(raw);
  const eligible = ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"].includes(String(body.subscriptionState));
  const items = Array.isArray(body.lineItems) ? body.lineItems.map(record) : [];
  const current = items.filter(x => products[String(x.productId)] && Date.parse(String(x.expiryTime)) > now)
    .sort((a, b) => (products[String(b.productId)] === "PREMIUM" ? 1 : 0) - (products[String(a.productId)] === "PREMIUM" ? 1 : 0))[0];
  const account = body.externalAccountIdentifiers ? record(body.externalAccountIdentifiers).obfuscatedExternalAccountId : null;
  return {
    tier: eligible && current ? products[String(current.productId)] : "FREE",
    expiresAt: eligible && current ? Math.min(Date.parse(String(current.expiryTime)), now + 24 * 60 * 60 * 1000) : 0,
    account: typeof account === "string" && account.length <= 128 ? account : null,
    linkedToken: typeof body.linkedPurchaseToken === "string" ? body.linkedPurchaseToken : null,
    acknowledgeProduct: eligible && current && body.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING" ? String(current.productId) : null,
  };
}
