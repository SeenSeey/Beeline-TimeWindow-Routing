import axios from "axios";
import { toApiParams, type RequestsQuery } from "../model/query";
import type { ApiResponse, PageResponse, RegionInfo, RequestDetailed, RequestItem } from "../types";

export { toApiFailure, type ApiFailure } from "@/features/timeline/api/timelineApi";

export async function fetchRequests(query: RequestsQuery): Promise<PageResponse<RequestItem>> {
  const response = await axios.get<ApiResponse<PageResponse<RequestItem>>>("/api/v1/requests", {
    params: toApiParams(query),
  });
  const page = response.data?.payload;
  return page ?? { payload: [], page: query.page, limit: query.limit, totalElements: 0, totalPages: 0 };
}

/** null when the server answered without a request */
export async function fetchRequest(id: number): Promise<RequestDetailed | null> {
  const response = await axios.get<ApiResponse<RequestDetailed>>(`/api/v1/requests/${id}`);
  return response.data?.payload ?? null;
}

export async function fetchRegions(): Promise<RegionInfo[]> {
  const response = await axios.get<ApiResponse<RegionInfo[]>>("/api/v1/regions");
  return response.data?.payload ?? [];
}
