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
export type ScheduleStatusKeyword = "CALCULATING" | "RECALCULATING" | "CREATED";

export type JobType = Dictionary<JobTypeKeyword>;
export type RequestStatus = Dictionary<RequestStatusKeyword>;
export type Vehicle = Dictionary<VehicleKeyword>;
export type Skill = Dictionary<SkillKeyword>;
export type ScheduleStatus = Dictionary<ScheduleStatusKeyword>;

export type SlotType = "REQUEST" | "TRAVEL" | "BREAK";

export type SlotAlterationAction =
  | "ADDED_FOR_HIGHER_PRIORITY_REQUEST"
  | "MOVED_FOR_HIGHER_PRIORITY_REQUEST"
  | "REMOVED_FOR_HIGHER_PRIORITY_REQUEST"
  | "ADDED_AFTER_REQUEST_CANCELLATION"
  | "MOVED_AFTER_REQUEST_CANCELLATION"
  | "REMOVED_AFTER_REQUEST_CANCELLATION"
  | "ADDED_FOR_CREW_OFF_SHIFT"
  | "MOVED_FOR_CREW_OFF_SHIFT"
  | "REMOVED_FOR_CREW_OFF_SHIFT";

export interface SlotAlterationReason {
  id: number;
  action: SlotAlterationAction;
  description: string;
  affectedRequestId: number;
  slotId: number;
  slotScheduleId: number;
  causeRequestId: number | null;
  causeCrewId: number | null;
  crewId: number | null;
  crewName: string | null;
  plannedStart: string | null;
  plannedEnd: string | null;
}

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
  /** display name of the equipment the request needs; null if none */
  requiredEquipment: string | null;
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
  /** what the crew can do; shown in edit mode */
  skills: Skill[];
  /** equipment the crew has on hand now; a request needing something missing here can't be moved to the crew */
  equipment: EquipmentOnHand[];
  slots: Slot[];
  /** taken off shift for the schedule's date: shown on the board, but takes no requests */
  offShift?: boolean;
}

/** EquipmentDto */
export interface EquipmentOnHand {
  id: number;
  displayName: string;
  quantity: number;
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

/** ScheduleEditDto.Stop — one visit of an edited route */
export interface DraftStop {
  requestId: number;
  /** "HH:mm:ss" the work starts at, as placed on the timeline; null: as early as possible */
  startTime: string | null;
}

/** ScheduleEditDto.CrewRoute — a crew's stops in visit order */
export interface CrewRouteDraft {
  crewId: number;
  stops: DraftStop[];
}

/** SchedulePreviewDto.Violation — a visit that breaks a rule (time window, shift, skill, vehicle) */
export interface RouteViolation {
  crewId: number;
  requestId: number;
  message: string;
}

/** SchedulePreviewDto — POST /api/v1/schedules/regions/{id}/preview */
export interface SchedulePreview {
  /** recalculated rows of the edited crews */
  engineers: Engineer[];
  /** the region's requests left without a crew after the edit */
  unassignedRequests: UnassignedRequest[];
  /** the edit can only be saved when this is empty */
  violations: RouteViolation[];
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
  /** reasons produced only by the automatic transition to this schedule version */
  alterationReasons: SlotAlterationReason[];
}
