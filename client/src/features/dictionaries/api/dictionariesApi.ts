import axios from "axios";
import type { ApiResponse } from "@/features/timeline/types";
import type { DictionaryItem } from "../types";

export { toApiFailure } from "@/features/timeline/api/timelineApi";

/** The dictionaries the server has, by their path under /api/v1/dictionaries */
export type DictionaryName = "request-statuses" | "job-types" | "vehicles" | "skills" | "equipment";

export async function fetchDictionary(name: DictionaryName): Promise<DictionaryItem[]> {
  const response = await axios.get<ApiResponse<DictionaryItem[]>>(`/api/v1/dictionaries/${name}`);
  return response.data?.payload ?? [];
}
