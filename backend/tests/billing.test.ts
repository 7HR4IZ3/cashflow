import { expect, test } from "bun:test";
import { convexTest } from "convex-test";
import schema from "../convex/schema";
import { internal } from "../convex/_generated/api";
const modules = {
  "../convex/billing.ts": () => import("../convex/billing"),
  "../convex/_generated/server.js": () => import("../convex/_generated/server.js"),
};
const args = (id: string) => ({ root: "verified-root", month: "2026-10", requestId: id, fingerprint: "hash", tier: "LITE" as const, lease: id });
test("duplicate deliveries reserve once; successful replay is free", async () => {
  const t = convexTest(schema, modules);
  const first = await t.mutation(internal.billing.reserve, args("one"));
  expect(first.status).toBe("reserved");
  expect((await t.mutation(internal.billing.reserve, args("one"))).status).toBe("busy");
  await t.mutation(internal.billing.finish, { root: "verified-root", requestId: "one", lease: "one", result: { relevant: true, direction: "IN", kind: null, model: "jev-1.13.0" } });
  expect((await t.mutation(internal.billing.reserve, args("one"))).status).toBe("cached");
  expect(await t.query(internal.billing.usage, { root: "verified-root", month: "2026-10" })).toBe(1);
  expect((await t.mutation(internal.billing.reserve, { ...args("one"), fingerprint: "different" })).status).toBe("conflict");
});
test("failed requests refund once and a stale callback cannot complete a new attempt", async () => {
  const t = convexTest(schema, modules);
  await t.mutation(internal.billing.reserve, args("one"));
  await t.mutation(internal.billing.finish, { root: "verified-root", requestId: "one", lease: "one", result: null });
  await t.mutation(internal.billing.finish, { root: "verified-root", requestId: "one", lease: "one", result: null });
  expect(await t.query(internal.billing.usage, { root: "verified-root", month: "2026-10" })).toBe(0);
  await t.mutation(internal.billing.reserve, { ...args("one"), lease: "retry" });
  await t.mutation(internal.billing.finish, { root: "verified-root", requestId: "one", lease: "one", result: null });
  expect(await t.query(internal.billing.usage, { root: "verified-root", month: "2026-10" })).toBe(1);
});
test("allowances cannot be exceeded, and month rollover is independent", async () => {
  const t = convexTest(schema, modules);
  await t.run(async ctx => { await ctx.db.insert("usage", { root: "verified-root", month: "2026-10", used: 299 }); });
  expect((await t.mutation(internal.billing.reserve, args("last"))).status).toBe("reserved");
  expect((await t.mutation(internal.billing.reserve, args("extra"))).status).toBe("limit");
  expect((await t.mutation(internal.billing.reserve, { ...args("november"), month: "2026-11" })).status).toBe("reserved");
  expect((await t.mutation(internal.billing.reserve, { ...args("free"), tier: "FREE" })).status).toBe("limit");
});
test("replacement tokens share the same usage identity and revoke the old token", async () => {
  const t = convexTest(schema, modules);
  const root = await t.mutation(internal.billing.link, { tokenHash: "old", accountHash: "account", linkedHash: null });
  expect(await t.mutation(internal.billing.link, { tokenHash: "new", accountHash: "new-install", linkedHash: "old" })).toBe(root);
  expect(t.mutation(internal.billing.link, { tokenHash: "old", accountHash: "account", linkedHash: null })).rejects.toThrow();
});

test("a replacement first seen before its old token leaves a revocation tombstone", async () => {
  const t = convexTest(schema, modules);
  await t.mutation(internal.billing.link, { tokenHash: "new-first", accountHash: "account", linkedHash: "old-unseen" });
  await expect(t.mutation(internal.billing.link, { tokenHash: "old-unseen", accountHash: "account", linkedHash: null })).rejects.toThrow();
});
