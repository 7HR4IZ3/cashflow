import { defineApp } from "convex/server";
import rateLimiter from "@convex-dev/rate-limiter/convex.config.js";
import { v } from "convex/values";
const app = defineApp({ env: {
  GOOGLE_SERVICE_ACCOUNT_JSON: v.optional(v.string()),
  TYPESAFE_API_KEY: v.optional(v.string()),
  JEV_MODEL: v.optional(v.string()),
  ENABLE_PURCHASES: v.optional(v.union(v.literal("true"), v.literal("false"))),
} });
app.use(rateLimiter);
export default app;
