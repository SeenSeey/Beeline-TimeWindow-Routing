import type { CrewRouteDraft, DraftStop, ScheduleData, SchedulePreview } from "../types";
import { toMin } from "./format";
import type { CrewRow } from "./timeline";

// A schedule edit is a draft of every crew's route: its stops in visit order, each with the start time
// the dispatcher placed it at (null: as early as possible). Only the crews whose route differs from the
// saved schedule are sent to the server, which checks them.

/** crew id → stops in visit order */
export type RouteDraft = Map<number, DraftStop[]>;

/** drag-and-drop payload type, see DragPayload */
export const DRAG_REQUEST_TYPE = "application/x-schedule-request";

/** Minutes a dropped start time snaps to */
export const DROP_SNAP_MINUTES = 5;

/** What is dragged: a request, and how far from the bar's left edge it was grabbed (0 from the list) */
export interface DragPayload {
  requestId: number;
  grabOffsetPx: number;
}

export const encodeDragPayload = (payload: DragPayload): string => JSON.stringify(payload);

export function decodeDragPayload(data: string): DragPayload | null {
  try {
    const parsed = JSON.parse(data) as Partial<DragPayload>;
    if (!Number.isInteger(parsed.requestId)) return null;
    return { requestId: parsed.requestId as number, grabOffsetPx: Number(parsed.grabOffsetPx) || 0 };
  } catch {
    return null;
  }
}

/** Axis minutes (may run past midnight) → "HH:mm:ss", snapped to DROP_SNAP_MINUTES */
export function axisMinuteToTime(axisMinute: number): string {
  const snapped = Math.round(axisMinute / DROP_SNAP_MINUTES) * DROP_SNAP_MINUTES;
  const minutesOfDay = ((snapped % 1440) + 1440) % 1440;
  const hours = String(Math.floor(minutesOfDay / 60)).padStart(2, "0");
  const minutes = String(minutesOfDay % 60).padStart(2, "0");
  return `${hours}:${minutes}:00`;
}

/** The saved routes, in the visit order the timeline shows; every visit keeps its current start */
export function draftFromCrews(crews: CrewRow[]): RouteDraft {
  return new Map(
    crews.map((crew) => [
      crew.id,
      crew.requests.map((visit) => ({ requestId: visit.slot.requestId, startTime: visit.slot.startSlotTime })),
    ]),
  );
}

/** The draft without the request, wherever it was */
export function withoutRequest(draft: RouteDraft, requestId: number): RouteDraft {
  return new Map([...draft].map(([crewId, stops]) => [crewId, stops.filter((stop) => stop.requestId !== requestId)]));
}

/**
 * The draft with the request on the crew's route at `index` (counted without the request itself),
 * starting at `startTime` (null: as early as possible), taken off any route it was on — so the same
 * call adds, moves and reorders.
 */
export function withRequestAt(
  draft: RouteDraft,
  requestId: number,
  crewId: number,
  index: number,
  startTime: string | null,
): RouteDraft {
  const next = withoutRequest(draft, requestId);
  const stops = [...(next.get(crewId) ?? [])];
  stops.splice(Math.max(0, Math.min(index, stops.length)), 0, { requestId, startTime });
  next.set(crewId, stops);
  return next;
}

const sameStops = (first: DraftStop[], second: DraftStop[]): boolean =>
  first.length === second.length &&
  first.every((stop, i) => stop.requestId === second[i]?.requestId && stop.startTime === second[i]?.startTime);

/** The crews whose route differs from the saved one, as the server expects them */
export function changedRoutes(saved: RouteDraft, draft: RouteDraft): CrewRouteDraft[] {
  const changed: CrewRouteDraft[] = [];
  for (const [crewId, stops] of draft) {
    if (!sameStops(saved.get(crewId) ?? [], stops)) changed.push({ crewId, stops });
  }
  return changed;
}

/**
 * Where a request goes in a route when no drop position is given: after the visits whose requested
 * window opens no later than its own, so the route keeps roughly the order of the windows.
 */
export function indexByWindow(crew: CrewRow, requestId: number, windowStart: string): number {
  const opensAt = toMin(windowStart);
  return crew.requests.filter(
    (visit) => visit.slot.requestId !== requestId && toMin(visit.slot.requestTimeWindowStart) <= opensAt,
  ).length;
}

/** Where a dropped request goes: before the first visit that starts after the drop point (axis minutes) */
export function indexByDropMinute(crew: CrewRow, requestId: number, dropMinute: number): number {
  return crew.requests.filter((visit) => {
    if (visit.slot.requestId === requestId) return false;
    const segment = crew.segments.find((s) => s.kind === "work" && s.visit === visit);
    return segment ? segment.from < dropMinute : false;
  }).length;
}

/** The saved schedule with the edited crews' rows and the unassigned list replaced by the preview */
export function applyPreview(schedule: ScheduleData, preview: SchedulePreview): ScheduleData {
  const editedRows = new Map(preview.engineers.map((row) => [row.id, row]));
  return {
    ...schedule,
    engineers: schedule.engineers.map((row) => editedRows.get(row.id) ?? row),
    unassignedRequests: preview.unassignedRequests,
  };
}
