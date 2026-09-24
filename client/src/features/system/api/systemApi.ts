import axios from "axios";
import type { ApiResponse } from "@/features/timeline/types";

export { toApiFailure, type ApiFailure } from "@/features/timeline/api/timelineApi";

/**
 * Starts today's schedule calculation for every region; returns how many schedules were created.
 * The server answers 409 when any schedule for today already exists. The schedules then compute in
 * the background (status CALCULATING).
 */
export async function initializeSchedules(): Promise<number> {
  const response = await axios.post<ApiResponse<unknown[]>>("/api/v1/schedules/initialize");
  return response.data?.payload?.length ?? 0;
}

/**
 * Resets the working state: deletes all schedules with their slots and the request action audit,
 * and returns every request to IDLE without a crew (assignment, completion and cancellation cleared).
 */
export async function resetSchedules(): Promise<void> {
  await axios.post<ApiResponse<null>>("/api/v1/schedules/reset");
}
