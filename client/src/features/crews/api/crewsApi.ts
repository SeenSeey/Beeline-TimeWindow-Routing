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

export async function fetchRegions(): Promise<RegionInfo[]> {
  const response = await axios.get<ApiResponse<RegionInfo[]>>("/api/v1/regions");
  return response.data?.payload ?? [];
}
