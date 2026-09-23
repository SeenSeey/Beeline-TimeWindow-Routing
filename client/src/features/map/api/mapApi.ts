import axios from "axios";
import type { ApiResponse, RequestPoint } from "../types";

export { toApiFailure, type ApiFailure } from "@/features/timeline/api/timelineApi";

export async function fetchRequestPoints(regionId: number): Promise<RequestPoint[]> {
  const response = await axios.get<ApiResponse<RequestPoint[]>>("/api/v1/requests/map", { params: { regionId } });
  return response.data?.payload ?? [];
}
