import { cronJobs } from "convex/server";
import { internal } from "./_generated/api";
const crons = cronJobs();
crons.interval("Expire AI request metadata", { minutes: 10 }, internal.billing.cleanup, {});
export default crons;
