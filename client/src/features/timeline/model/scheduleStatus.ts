import type { ScheduleStatusKeyword } from "../types";

/**
 * The server is still working on the schedule (first calculation or a recalculation): it is refetched
 * until it is CREATED, has no unassigned list yet and can't be edited
 */
export const isScheduleCalculating = (keyword: ScheduleStatusKeyword): boolean =>
  keyword === "CALCULATING" || keyword === "RECALCULATING";
