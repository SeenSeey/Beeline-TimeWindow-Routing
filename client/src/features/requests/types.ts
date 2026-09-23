// Server DTOs of the requests feature

export type { ApiResponse, RegionInfo, RequestStatusKeyword, JobTypeKeyword } from "@/features/timeline/types";
import type {
  JobType,
  JobTypeKeyword,
  RegionInfo,
  RequestStatus,
  RequestStatusKeyword,
  ScheduleStatus,
  Skill,
  Vehicle,
} from "@/features/timeline/types";

/** RequestDto.AssignedCrewDto */
export interface AssignedCrew {
  id: number;
  name: string;
}

/** RequestDto — an item of GET /api/v1/requests */
export interface RequestItem {
  id: number;
  fullAddress: string;
  /** status keyword, e.g. "IDLE" */
  status: RequestStatusKeyword;
  urgent: boolean;
  region: RegionInfo;
  jobType: JobType;
  /** null while no crew is assigned */
  assignedCrew: AssignedCrew | null;
}

/** RequestDetailedDto.JobTypeDto */
export interface JobTypeDetails {
  keyword: JobTypeKeyword;
  displayName: string;
  jobTimeSpanMinutes: number;
  maxTravelTimeSpanMinutes: number;
  totalTimeSpanMinutes: number;
  requiredSkills: Skill[];
}

/** RequestDetailedDto.CrewDto */
export interface CrewDetails {
  id: number;
  name: string;
  /** "HH:mm:ss" */
  shiftStartTime: string;
  shiftEndTime: string;
  vehicle: Vehicle | null;
  skills: Skill[];
  /** equipment display names */
  equipment: string[];
}

/** RequestDetailedDto.ScheduledVisitDto — the request's work slot in one schedule */
export interface ScheduledVisit {
  scheduleId: number;
  /** "YYYY-MM-DD" */
  targetDate: string;
  scheduleStatus: ScheduleStatus;
  /** "HH:mm:ss" */
  arrivalTime: string | null;
  startTime: string | null;
  endTime: string | null;
  /** ISO LocalDateTime */
  finishedAt: string | null;
}

/** RequestDetailedDto — GET /api/v1/requests/{id} */
export interface RequestDetailed {
  id: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  status: RequestStatus | null;
  urgent: boolean;
  /** ISO LocalDateTime */
  deadline: string;
  region: RegionInfo;
  jobType: JobTypeDetails;
  requiredVehicle: Vehicle | null;
  /** equipment display names */
  equipment: string[];
  /** "HH:mm:ss" */
  requestTimeWindowStart: string;
  requestTimeWindowEnd: string;
  assignedCrew: CrewDetails | null;
  /** ISO LocalDateTime */
  assignedAt: string | null;
  createdAt: string;
  completedAt: string | null;
  canceledAt: string | null;
  /** newest schedule first */
  scheduledVisits: ScheduledVisit[];
}

/** PageResponseDto<T>: `page` is 1-based */
export interface PageResponse<T> {
  payload: T[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}

/** RequestSearchDto — query params of GET /api/v1/requests; null means "any" */
export interface RequestFilters {
  statusId: number | null;
  urgent: boolean | null;
  regionId: number | null;
  jobTypeId: number | null;
  /** true: only requests without a crew, false: only assigned ones */
  noAssignedCrew: boolean | null;
}
