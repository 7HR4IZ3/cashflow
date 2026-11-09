import { v } from "convex/values";
export const tier = v.union(v.literal("FREE"), v.literal("LITE"), v.literal("PREMIUM"));
export const suggestion = v.object({
  relevant: v.union(v.boolean(), v.null()),
  direction: v.union(v.literal("IN"), v.literal("OUT"), v.null()),
  kind: v.union(v.literal("INCOME"), v.literal("EXPENSE"), v.literal("TRANSFER"), v.literal("REFUND"), v.literal("FEE"), v.literal("UNCLASSIFIED"), v.null()),
  model: v.string(),
});
