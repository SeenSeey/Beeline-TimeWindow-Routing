// Server DTOs of the crews feature

export type { ApiResponse, RegionInfo, SkillKeyword, VehicleKeyword } from "@/features/timeline/types";
export type { PageResponse } from "@/features/requests/types";
import type {
  JobType,
  RegionInfo,
  RequestStatus,
  Skill,
  SkillKeyword,
  VehicleKeyword,
} from "@/features/timeline/types";

/** EquipmentDto */
export interface CrewEquipment {
  id: number;
  displayName: string;
  quantity: number;
}

/** CrewDetailedDto.VehicleDto */
export interface CrewVehicle {
  keyword: VehicleKeyword;
  displayName: string;
  averageSpeedKmh: number;
}

/** CrewDetailedDto.RouteVisitDto — one work slot of the crew's route */
export interface RouteVisit {
  requestId: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  jobType: JobType;
  status: RequestStatus | null;
  urgent: boolean;
  /** "HH:mm:ss" */
  arrivalTime: string | null;
  startTime: string | null;
  endTime: string | null;
  requestTimeWindowStart: string;
  requestTimeWindowEnd: string;
  /** ISO LocalDateTime */
  finishedAt: string | null;
}

/** CrewDetailedDto.AssignedRequestDto */
export interface CrewAssignedRequest {
  id: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  status: RequestStatus | null;
  urgent: boolean;
  jobType: JobType;
  /** ISO LocalDateTime */
  deadline: string;
  assignedAt: string | null;
  completedAt: string | null;
  canceledAt: string | null;
}

/** CrewDetailedDto — GET /api/v1/crews/{id} */
export interface CrewDetailed {
  id: number;
  name: string;
  region: RegionInfo;
  /** "HH:mm:ss" */
  shiftStartTime: string;
  shiftEndTime: string;
  vehicle: CrewVehicle | null;
  skills: Skill[];
  /** what the crew has on hand now: issued with the day's schedule, minus requests taken into work */
  equipment: CrewEquipment[];
  /** "YYYY-MM-DD" — the date of the route below */
  routeDate: string;
  /** the crew's work slots in that date's schedule, in visit order */
  route: RouteVisit[];
  /** latest assignment first */
  assignedRequests: CrewAssignedRequest[];
}

/** CrewDto — an item of GET /api/v1/crews */
export interface CrewItem {
  id: number;
  name: string;
  regionName: string;
  /** vehicle keyword, e.g. "CAR" */
  vehicle: VehicleKeyword;
  /** skill keywords, e.g. ["LOCAL_WORK"] */
  skills: SkillKeyword[];
}

/** CrewSearchDto — query params of GET /api/v1/crews; null means "any" */
export interface CrewFilters {
  /** part of the crew name, case-insensitive */
  name: string | null;
  regionId: number | null;
  vehicleId: number | null;
  skillId: number | null;
}
