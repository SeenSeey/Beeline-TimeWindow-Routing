export type SlotType = "REQUEST" | "TRAVEL" | "BREAK";

export interface Slot {
  requestId: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  slotType: SlotType;
  /** status display name */
  status: string | null;
  /** ISO LocalDateTime */
  deadline: string | null;
  /** job type display name */
  jobType: string;
  /** vehicle display name */
  requiredVehicle: string | null;
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
  slots: Slot[];
}

/** RegionDto — part of the schedule response and the items of GET /api/v1/regions */
export interface RegionInfo {
  id: number;
  displayName: string;
  officeAddress: string;
}

/** Active request of the region that did not get a place in the schedule */
export interface UnassignedRequest {
  id: number;
  fullAddress: string;
  latitude: number;
  longitude: number;
  /** status display name */
  status: string | null;
  urgent: boolean;
  /** ISO LocalDateTime */
  deadline: string;
  /** job type display name */
  jobType: string;
  jobTimeSpanMinutes: number;
  maxTravelTimeSpanMinutes: number;
  /** skill display names */
  requiredSkills: string[];
  /** equipment display names */
  equipment: string[];
  /** vehicle display name */
  requiredVehicle: string | null;
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
  /** schedule status display name, e.g. "Формируется" | "Готово" */
  status: string;
  region: RegionInfo;
  engineers: Engineer[];
  /** empty while the schedule is still being calculated */
  unassignedRequests: UnassignedRequest[];
}

/** ServerResponse<T>: on errors `error` holds the domain error code and `payload` is null */
export interface ApiResponse<T> {
  statusCode: number;
  error: string | null;
  message: string | null;
  payload: T | null;
}
