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

/** A status change of a request, as the server's PATCH /api/v1/requests/{id}/<action> endpoints name them */
export type RequestStatusAction = "in_progress" | "complete" | "cancel";

/**
 * Changes the request's status (204, no body). The server answers 409 when the request's current status
 * doesn't allow it: work can't start once in progress, completed or canceled; a completed or canceled
 * request can't be completed; only a request that hasn't started (not in progress, completed or canceled)
 * can be canceled.
 */
export async function changeRequestStatus(id: number, action: RequestStatusAction): Promise<void> {
  await axios.patch(`/api/v1/requests/${id}/${action}`);
}

export async function fetchRegions(): Promise<RegionInfo[]> {
  const response = await axios.get<ApiResponse<RegionInfo[]>>("/api/v1/regions");
  return response.data?.payload ?? [];
}
