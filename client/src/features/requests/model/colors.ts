import type { RequestStatusKeyword } from "../types";

export { jobTypeColor, scheduleStatusColor } from "@/features/timeline/model/colors";

// Styles are keyed by server enum keywords; display names are only ever shown, never matched.

const STATUS_COLORS: Record<RequestStatusKeyword, string> = {
  IDLE: "var(--job-addon)",
  ASSIGNED: "var(--accent)",
  IN_PROGRESS: "var(--job-installation)",
  COMPLETED: "var(--job-local)",
  CANCELED: "var(--faint)",
};

/** unknown keywords (a server enum grew) stay visible in a neutral colour */
export const statusColor = (keyword: string): string =>
  STATUS_COLORS[keyword as RequestStatusKeyword] ?? "var(--muted)";
