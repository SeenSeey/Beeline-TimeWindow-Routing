import axios from "axios";
import type { ApiResponse, CrewRouteDraft, RegionInfo, ScheduleData, SchedulePreview } from "../types";

export interface ApiFailure {
  kind: "not-found" | "error";
  /** message from the server's error response, when there is one */
  message: string | null;
  status: number | null;
}

export async function fetchRegions(): Promise<RegionInfo[]> {
  const response = await axios.get<ApiResponse<RegionInfo[]>>("/api/v1/regions");
  return response.data?.payload ?? [];
}

/** null when the server answered without a schedule */
export async function fetchSchedule(regionId: number): Promise<ScheduleData | null> {
  const response = await axios.get<ApiResponse<ScheduleData>>(`/api/v1/schedules/regions/${regionId}`);
  return response.data?.payload ?? null;
}

/**
 * Recalculates the edited crews' routes of today's schedule without saving them. `routes` lists only
 * the crews that changed; a request dropped from their routes leaves the schedule.
 */
export async function previewScheduleEdit(regionId: number, routes: CrewRouteDraft[]): Promise<SchedulePreview> {
  const response = await axios.post<ApiResponse<SchedulePreview>>(`/api/v1/schedules/regions/${regionId}/preview`, {
    routes,
  });
  return response.data?.payload ?? { engineers: [], unassignedRequests: [], violations: [] };
}

/** Saves the edited routes; the server re-checks them (400 if a visit breaks a rule) and returns the updated schedule */
export async function saveScheduleEdit(regionId: number, routes: CrewRouteDraft[]): Promise<ScheduleData | null> {
  const response = await axios.put<ApiResponse<ScheduleData>>(`/api/v1/schedules/regions/${regionId}/routes`, {
    routes,
  });
  return response.data?.payload ?? null;
}

// axios rejects every non-2xx response; the server still sends a ServerResponse body with the reason
export function toApiFailure(e: unknown): ApiFailure {
  const response = axios.isAxiosError<ApiResponse<null>>(e) ? e.response : undefined;
  const status = response?.status ?? null;
  if (status !== 404) console.error("Ошибка при получении данных", e);
  return { kind: status === 404 ? "not-found" : "error", message: response?.data?.message ?? null, status };
}
