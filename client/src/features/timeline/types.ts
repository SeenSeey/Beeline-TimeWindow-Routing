// Server DTOs of the timeline feature

/** ServerResponse<T>: on errors `error` holds the domain error code and `payload` is null */
export interface ApiResponse<T> {
  statusCode: number;
  error: string | null;
  message: string | null;
  payload: T | null;
}

/** RegionDto — part of the schedule response and the items of GET /api/v1/regions */
export interface RegionInfo {
  id: number;
  displayName: string;
  officeAddress: string;
}

/** DictionaryDto: `keyword` is the server enum name (logic, styling), `displayName` is for display */
export interface Dictionary<K extends string> {
  keyword: K;
  displayName: string;
}

// Keywords mirror the server enums
export type JobTypeKeyword = "LOCAL_WORK" | "INSTALLATION" | "ADDON" | "EMERGENCY";
export type RequestStatusKeyword = "IDLE" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELED";
export type VehicleKeyword = "PEDESTRIAN" | "BICYCLE" | "PUBLIC_TRANSPORT" | "CAR";
export type SkillKeyword = "LOCAL_WORK" | "INSTALLATION_OR_ADDON" | "EMERGENCY";
export type ScheduleStatusKeyword = "CALCULATING" | "CREATED";

export type JobType = Dictionary<JobTypeKeyword>;
export type RequestStatus = Dictionary<RequestStatusKeyword>;
export type Vehicle = Dictionary<VehicleKeyword>;
export type Skill = Dictionary<SkillKeyword>;
export type ScheduleStatus = Dictionary<ScheduleStatusKeyword>;

export type SlotType = "REQUEST" | "TRAVEL" | "BREAK";

export interface Slot {
  requestId: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  slotType: SlotType;
  status: RequestStatus | null;
  /** ISO LocalDateTime */
  deadline: string | null;
  jobType: JobType;
  requiredVehicle: Vehicle | null;
  /** "HH:mm:ss" — when the crew arrives at the address; work may wait until startSlotTime */
  arrivalTime: string | null;
  /** "HH:mm:ss" */
  startSlotTime: string;
  endSlotTime: string;
  requestTimeWindowStart: string;
  requestTimeWindowEnd: string;
  createdAt: string | null;
  completedAt: string | null;
  canceledAt: string | null;
}

export interface Engineer {
  id: number;
  name: string;
  shiftStartTime: string;
  shiftEndTime: string;
  /** the crew's vehicle; null if the server has none on record */
  vehicle: Vehicle | null;
  slots: Slot[];
}

/** Active request of the region that did not get a place in the schedule */
export interface UnassignedRequest {
  id: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  status: RequestStatus | null;
  urgent: boolean;
  /** ISO LocalDateTime */
  deadline: string;
  jobType: JobType;
  jobTimeSpanMinutes: number;
  maxTravelTimeSpanMinutes: number;
  requiredSkills: Skill[];
  /** equipment display names */
  equipment: string[];
  requiredVehicle: Vehicle | null;
  /** "HH:mm:ss" */
  requestTimeWindowStart: string;
  requestTimeWindowEnd: string;
  /** name of a crew the request was assigned to outside this schedule */
  assignedCrew: string | null;
  assignedAt: string | null;
  createdAt: string;
}

export interface ScheduleData {
  /** "YYYY-MM-DD" */
  targetDate: string;
  createdAt: string | null;
  status: ScheduleStatus;
  region: RegionInfo;
  engineers: Engineer[];
  /** empty while the schedule is still being calculated */
  unassignedRequests: UnassignedRequest[];
}
