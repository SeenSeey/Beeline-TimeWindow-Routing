import axios from "axios";
import { toApiParams, type CrewsQuery } from "../model/query";
import type { ApiResponse, CrewDetailed, CrewItem, PageResponse, RegionInfo } from "../types";

export { toApiFailure, type ApiFailure } from "@/features/timeline/api/timelineApi";

export async function fetchCrews(query: CrewsQuery): Promise<PageResponse<CrewItem>> {
  const response = await axios.get<ApiResponse<PageResponse<CrewItem>>>("/api/v1/crews", {
    params: toApiParams(query),
  });
  const page = response.data?.payload;
  return page ?? { payload: [], page: query.page, limit: query.limit, totalElements: 0, totalPages: 0 };
}

/** null when the server answered without a crew */
export async function fetchCrew(id: number): Promise<CrewDetailed | null> {
  const response = await axios.get<ApiResponse<CrewDetailed>>(`/api/v1/crews/${id}`);
  return response.data?.payload ?? null;
}

/**
 * Takes the crew off shift for today (204, no body); the server then recalculates the schedule without it.
 * Answers 409 when the crew is already off shift today, 404 when there is no such crew.
 */
export async function takeCrewOffShift(id: number): Promise<void> {
  await axios.patch(`/api/v1/crews/${id}/offShift`);
}

export async function fetchRegions(): Promise<RegionInfo[]> {
  const response = await axios.get<ApiResponse<RegionInfo[]>>("/api/v1/regions");
  return response.data?.payload ?? [];
}
