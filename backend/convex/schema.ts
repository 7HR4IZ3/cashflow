import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";
import { suggestion } from "./validators";
export default defineSchema({
  purchases: defineTable({ tokenHash: v.string(), root: v.string(), revoked: v.boolean() }).index("by_tokenHash", ["tokenHash"]),
  usage: defineTable({ root: v.string(), month: v.string(), used: v.number() }).index("by_root_and_month", ["root", "month"]),
  requests: defineTable({
    root: v.string(), requestId: v.string(), fingerprint: v.string(), month: v.string(),
    state: v.union(v.literal("pending"), v.literal("done"), v.literal("failed")),
    lease: v.string(), updatedAt: v.number(), result: v.union(suggestion, v.null()),
  }).index("by_root_and_requestId", ["root", "requestId"]).index("by_updatedAt", ["updatedAt"]).index("by_state_and_updatedAt", ["state", "updatedAt"]),
});
