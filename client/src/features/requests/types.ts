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
  /** what happened to the request, from the audit table; newest first */
  history: RequestHistoryItem[];
}

/** RequestActionAudit.Action — mirrors the server enum */
export type RequestHistoryAction =
  | "REQUEST_CREATED"
  | "ASSIGNED"
  | "REASSIGNED"
  | "RESCHEDULED"
  | "REMOVED_FROM_SCHEDULE"
  | "STARTED"
  | "COMPLETED"
  | "CANCELED";

/** RequestHistoryItemDto — one row of the request's audit */
export interface RequestHistoryItem {
  id: number;
  /** ISO LocalDateTime */
  timestamp: string;
  action: RequestHistoryAction;
  /** the action's display name, e.g. "Назначена бригаде" */
  description: string;
  /** the crew assigned by this action (assigned, reassigned); null otherwise */
  crewId: number | null;
  crewName: string | null;
  /** "HH:mm:ss" — the planned work (assigned, reassigned, rescheduled); null otherwise */
  plannedStart: string | null;
  plannedEnd: string | null;
}

/** PageResponseDto<T>: `page` is 1-based */
export interface PageResponse<T> {
  payload: T[];
  page: number;
  limit: number;
  totalElements: number;
  totalPages: number;
}

/**
 * RequestCreateDto — body of POST /api/v1/requests. The server geocodes the address, sets the deadline
 * by the job type and marks an EMERGENCY request urgent (which makes it recalculate today's schedule).
 */
export interface RequestCreateBody {
  regionId: number;
  address: string;
  jobTypeId: number;
  requiredVehicleId: number | null;
  requiredEquipmentId: number | null;
  /** "HH:mm:ss"; both or neither — without a window the request fits any time of day */
  startTimeWindow: string | null;
  endTimeWindow: string | null;
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
