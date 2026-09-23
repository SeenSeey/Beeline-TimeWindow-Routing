import axios from "axios";
import type { ApiResponse, RegionInfo, ScheduleData } from "../types";

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

// axios rejects every non-2xx response; the server still sends a ServerResponse body with the reason
export function toApiFailure(e: unknown): ApiFailure {
  const response = axios.isAxiosError<ApiResponse<null>>(e) ? e.response : undefined;
  const status = response?.status ?? null;
  if (status !== 404) console.error("Ошибка при получении данных", e);
  return { kind: status === 404 ? "not-found" : "error", message: response?.data?.message ?? null, status };
}
