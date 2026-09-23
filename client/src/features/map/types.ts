// Server DTOs of the map feature

export type { ApiResponse, JobType, RegionInfo, RequestStatus } from "@/features/timeline/types";
import type { JobType, RequestStatus } from "@/features/timeline/types";

/** RequestMapPointDto — an item of GET /api/v1/requests/map?regionId= */
export interface RequestPoint {
  id: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  status: RequestStatus | null;
  urgent: boolean;
  jobType: JobType;
  /** ISO LocalDateTime */
  deadline: string;
  /** "HH:mm:ss" */
  requestTimeWindowStart: string;
  requestTimeWindowEnd: string;
  /** crew name, null while unassigned */
  assignedCrew: string | null;
}
