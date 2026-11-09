import { describe, expect, test } from "bun:test";
import { interpretPurchase } from "../convex/lib/policy";
import { parseSuggestion } from "../convex/lib/jev";
const now = Date.parse("2026-10-30T00:00:00Z");
const purchase = (state: string, expiry = "2026-12-01T00:00:00Z", productId = "cashflow_lite") => ({ subscriptionState: state, acknowledgementState: "ACKNOWLEDGEMENT_STATE_PENDING", lineItems: [{ productId, expiryTime: expiry }] });
describe("Google is the entitlement authority", () => {
  test("active and grace purchases grant a bounded offline lease", () => {
    for (const state of ["ACTIVE", "IN_GRACE_PERIOD", "CANCELED"]) {
      expect(interpretPurchase(purchase(`SUBSCRIPTION_STATE_${state}`), now)).toMatchObject({ tier: "LITE", expiresAt: now + 86400000 });
    }
  });
  test("pending, hold, paused and expired never grant a paid plan", () => {
    for (const state of ["PENDING", "ON_HOLD", "PAUSED", "EXPIRED"]) expect(interpretPurchase(purchase(`SUBSCRIPTION_STATE_${state}`), now).tier).toBe("FREE");
    expect(interpretPurchase(purchase("SUBSCRIPTION_STATE_ACTIVE", "2026-10-29T00:00:00Z"), now).tier).toBe("FREE");
    expect(interpretPurchase(purchase("SUBSCRIPTION_STATE_ACTIVE", undefined, "forged_product"), now).tier).toBe("FREE");
  });
  test("does not grant a future deferred Premium line item", () => {
    const body = purchase("SUBSCRIPTION_STATE_ACTIVE");
    body.lineItems.push({ productId: "cashflow_premium", expiryTime: "" });
    expect(interpretPurchase(body, now).tier).toBe("LITE");
  });
});
const answer = (choice: string, confidence = 0.95, probability = 0.99) => ({ type: "choice", choice, confidence, probabilities: { [choice]: probability } });
const response = () => ({ model: "jev-1.13.0", answers: { relevance: answer("PAYMENT"), direction: answer("IN"), kind: answer("INCOME") } });
describe("Jev suggestions stay typed and uncertain answers abstain", () => {
  test("extracts only known labels, with no model-generated money", () => {
    expect(parseSuggestion(response())).toEqual({ relevant: true, direction: "IN", kind: "INCOME", model: "jev-1.13.0" });
  });
  test("low confidence or probability means no suggested direction", () => {
    for (const value of [answer("IN", 0.2), answer("IN", 0.95, 0.6)]) {
      const raw = response(); raw.answers.direction = value;
      expect(parseSuggestion(raw).direction).toBeNull();
    }
  });
  test("unknown labels and non-finite values fail closed", () => {
    const raw = response(); raw.answers.direction = answer("SEND_ALL_DATA");
    expect(() => parseSuggestion(raw)).toThrow();
    raw.answers.direction = answer("IN", NaN);
    expect(() => parseSuggestion(raw)).toThrow();
  });
});
