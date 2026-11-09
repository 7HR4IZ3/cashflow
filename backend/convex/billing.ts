import { internalMutation, internalQuery } from "./_generated/server";
import { v } from "convex/values";
import { tier, suggestion } from "./validators";
import { limits } from "./lib/policy";

export const link = internalMutation({
  args: { tokenHash: v.string(), accountHash: v.union(v.string(), v.null()), linkedHash: v.union(v.string(), v.null()) },
  returns: v.string(),
  handler: async (ctx, args) => {
    const existing = await ctx.db.query("purchases").withIndex("by_tokenHash", q => q.eq("tokenHash", args.tokenHash)).unique();
    if (existing?.revoked) throw new Error("Purchase was replaced");
    const previous = args.linkedHash ? await ctx.db.query("purchases").withIndex("by_tokenHash", q => q.eq("tokenHash", args.linkedHash!)).unique() : null;
    const root = previous?.root ?? existing?.root ?? args.accountHash ?? args.linkedHash ?? args.tokenHash;
    if (existing && existing.root !== root) await ctx.db.patch("purchases", existing._id, { root });
    if (!existing) await ctx.db.insert("purchases", { tokenHash: args.tokenHash, root, revoked: false });
    if (!previous && args.linkedHash && args.linkedHash !== args.tokenHash) {
      await ctx.db.insert("purchases", { tokenHash: args.linkedHash, root, revoked: true });
    }
    if (previous && previous.tokenHash !== args.tokenHash) await ctx.db.patch("purchases", previous._id, { revoked: true });
    return root;
  },
});
export const usage = internalQuery({
  args: { root: v.string(), month: v.string() }, returns: v.number(),
  handler: async (ctx, args) => (await ctx.db.query("usage").withIndex("by_root_and_month", q => q.eq("root", args.root).eq("month", args.month)).unique())?.used ?? 0,
});
export const reserve = internalMutation({
  args: { root: v.string(), month: v.string(), requestId: v.string(), fingerprint: v.string(), tier, lease: v.string() },
  returns: v.object({ status: v.union(v.literal("reserved"), v.literal("cached"), v.literal("busy"), v.literal("limit"), v.literal("conflict")), result: v.union(suggestion, v.null()) }),
  handler: async (ctx, args) => {
    const old = await ctx.db.query("requests").withIndex("by_root_and_requestId", q => q.eq("root", args.root).eq("requestId", args.requestId)).unique();
    if (old && old.fingerprint !== args.fingerprint) return { status: "conflict" as const, result: null };
    if (old?.state === "done") return { status: "cached" as const, result: old.result };
    if (old?.state === "pending") return { status: "busy" as const, result: null };
    const counter = await ctx.db.query("usage").withIndex("by_root_and_month", q => q.eq("root", args.root).eq("month", args.month)).unique();
    if ((counter?.used ?? 0) >= limits[args.tier]) return { status: "limit" as const, result: null };
    if (counter) await ctx.db.patch("usage", counter._id, { used: counter.used + 1 });
    else await ctx.db.insert("usage", { root: args.root, month: args.month, used: 1 });
    const request = { root: args.root, month: args.month, requestId: args.requestId, fingerprint: args.fingerprint, lease: args.lease, state: "pending" as const, updatedAt: Date.now(), result: null };
    if (old) await ctx.db.replace("requests", old._id, request); else await ctx.db.insert("requests", request);
    return { status: "reserved" as const, result: null };
  },
});
export const finish = internalMutation({
  args: { root: v.string(), requestId: v.string(), lease: v.string(), result: v.union(suggestion, v.null()) }, returns: v.null(),
  handler: async (ctx, args) => {
    const request = await ctx.db.query("requests").withIndex("by_root_and_requestId", q => q.eq("root", args.root).eq("requestId", args.requestId)).unique();
    if (!request || request.state !== "pending" || request.lease !== args.lease) return null;
    await ctx.db.patch("requests", request._id, { state: args.result ? "done" : "failed", result: args.result, updatedAt: Date.now() });
    if (!args.result) {
      const counter = await ctx.db.query("usage").withIndex("by_root_and_month", q => q.eq("root", args.root).eq("month", request.month)).unique();
      if (counter) await ctx.db.patch("usage", counter._id, { used: Math.max(0, counter.used - 1) });
    }
    return null;
  },
});
export const cleanup = internalMutation({
  args: {}, returns: v.null(),
  handler: async ctx => {
    // Bounded sweep; pending calls older than ten minutes are refunded after a crash.
    const pending = await ctx.db.query("requests").withIndex("by_state_and_updatedAt", q => q.eq("state", "pending").lt("updatedAt", Date.now() - 600000)).take(100);
    const expired = await ctx.db.query("requests").withIndex("by_updatedAt", q => q.lt("updatedAt", Date.now() - 40 * 86400000)).take(100);
    const stale = [...new Map([...pending, ...expired].map(row => [row._id, row])).values()];
    for (const request of stale) {
      if (request.state === "pending") {
        const counter = await ctx.db.query("usage").withIndex("by_root_and_month", q => q.eq("root", request.root).eq("month", request.month)).unique();
        if (counter) await ctx.db.patch("usage", counter._id, { used: Math.max(0, counter.used - 1) });
        await ctx.db.patch("requests", request._id, { state: "failed", updatedAt: Date.now() });
      } else if (request.updatedAt < Date.now() - 40 * 86400000) await ctx.db.delete("requests", request._id);
    }
    return null;
  },
});
