import type { Engineer, ScheduleData, Slot } from "../types";
import { jobTypeColor } from "./colors";
import { isOnDate, toMin } from "./format";

// ── Time axis ────────────────────────────────────────────────────────────────

export interface TimeAxis {
  /** minutes since midnight of the first hour on the axis */
  origin: number;
  /** minutes of the last hour; may exceed 1440 when shifts cross midnight */
  end: number;
  hours: number[];
  /** "HH:mm:ss" → minutes on the axis; times before the origin belong to the next day */
  norm: (t: string) => number;
}

/** 09:00–22:00 like the mockup, widened to fit every shift and slot */
export function buildAxis(engineers: Engineer[]): TimeAxis {
  const startH = Math.min(9, ...engineers.map((e) => Math.floor(toMin(e.shiftStartTime) / 60)));
  const origin = startH * 60;
  const norm = (t: string) => {
    const m = toMin(t);
    return m < origin ? m + 1440 : m;
  };
  const ends = engineers.flatMap((e) => [norm(e.shiftEndTime), ...e.slots.map((s) => norm(s.endSlotTime))]);
  const endH = Math.max(22, ...ends.map((m) => Math.ceil(m / 60)));
  return {
    origin,
    end: endH * 60,
    norm,
    hours: Array.from({ length: endH - startH + 1 }, (_, i) => startH + i),
  };
}

// ── Crew rows ────────────────────────────────────────────────────────────────

export interface Visit {
  slot: Slot;
  crewId: number;
  crewName: string;
  color: string;
  /** "HH:mm:ss" — slot.arrivalTime, or the work start when the server didn't send one */
  arrival: string;
  /** minutes of work */
  duration: number;
  /** deadline is the schedule's target date */
  urgent: boolean;
  /** work starts outside the requested window */
  outOfWindow: boolean;
}

export type Segment =
  | { key: string; kind: "travel" | "wait" | "break"; from: number; to: number }
  | { key: string; kind: "work"; from: number; to: number; visit: Visit };

export interface CrewRow extends Engineer {
  /** shift start/end in axis minutes */
  shift: [number, number];
  requests: Visit[];
  segments: Segment[];
}

export const visitKey = (v: Visit): string => `${v.crewId}-${v.slot.requestId}`;

/**
 * "HH:mm:ss" time of day falls inside the window; a window whose end is before its start runs over midnight.
 * Compares plain times of day — the axis' `norm` would push a window start before the axis origin to the next day.
 */
export function isWithinTimeWindow(time: string, windowStart: string, windowEnd: string): boolean {
  const timeMinutes = toMin(time);
  const windowStartMinutes = toMin(windowStart);
  const windowEndMinutes = toMin(windowEnd);

  const crossesMidnight = windowEndMinutes < windowStartMinutes;
  return crossesMidnight
    ? timeMinutes >= windowStartMinutes || timeMinutes <= windowEndMinutes
    : timeMinutes >= windowStartMinutes && timeMinutes <= windowEndMinutes;
}

export function buildCrewRows(schedule: ScheduleData, axis: TimeAxis): CrewRow[] {
  const { norm } = axis;

  const rows = schedule.engineers.map((e): CrewRow => {
    const slots = [...e.slots].sort((a, b) => norm(a.startSlotTime) - norm(b.startSlotTime));
    const requests: Visit[] = [];
    const segments: Segment[] = [];

    slots.forEach((s, i) => {
      const from = norm(s.startSlotTime);
      const to = norm(s.endSlotTime);
      if (s.slotType === "TRAVEL") {
        segments.push({ key: `t${i}`, kind: "travel", from, to });
        return;
      }
      if (s.slotType === "BREAK") {
        segments.push({ key: `b${i}`, kind: "break", from, to });
        return;
      }

      // Waiting for the window: the crew has arrived but the work hasn't started yet
      const arrival = s.arrivalTime ?? s.startSlotTime;
      if (norm(arrival) < from) segments.push({ key: `w${i}`, kind: "wait", from: norm(arrival), to: from });

      const visit: Visit = {
        slot: s,
        crewId: e.id,
        crewName: e.name,
        color: jobTypeColor(s.jobType.keyword),
        arrival,
        duration: to - from,
        urgent: isOnDate(s.deadline, schedule.targetDate),
        // Only the start matters: work may run past the window as long as it begins inside it
        outOfWindow: !isWithinTimeWindow(s.startSlotTime, s.requestTimeWindowStart, s.requestTimeWindowEnd),
      };
      requests.push(visit);
      segments.push({ key: `j${i}`, kind: "work", from, to, visit });
    });

    return { ...e, shift: [norm(e.shiftStartTime), norm(e.shiftEndTime)], requests, segments };
  });

  // Crews ordered by name, A→Я, whether on shift or not; numbers in names compare as numbers
  // ("Бригада 2" before "Бригада 10")
  return rows.sort((a, b) => crewNameCollator.compare(a.name, b.name) || a.id - b.id);
}

const crewNameCollator = new Intl.Collator("ru", { numeric: true, sensitivity: "base" });

// ── On-screen geometry ───────────────────────────────────────────────────────

export interface TimeScale {
  /** axis minutes → CSS left in % of the track */
  left: (m: number) => string;
  /** duration between two axis minutes → CSS width in % of the track */
  width: (a: number, b: number) => string;
  /** duration → px on the current track */
  px: (a: number, b: number) => number;
  /** whole-pixel box, for patterned fills that must start on a pixel boundary */
  snapped: (a: number, b: number) => { left: string; width: string };
}

export function createScale(axis: TimeAxis, trackWidth: number): TimeScale {
  const total = axis.end - axis.origin;
  const px = (a: number, b: number) => ((b - a) / total) * trackWidth;
  return {
    left: (m) => ((m - axis.origin) / total) * 100 + "%",
    width: (a, b) => ((b - a) / total) * 100 + "%",
    px,
    snapped: (a, b) => {
      const left = Math.round(px(axis.origin, a));
      const right = Math.round(px(axis.origin, b));
      return { left: left + "px", width: Math.max(right - left, 0) + "px" };
    },
  };
}
