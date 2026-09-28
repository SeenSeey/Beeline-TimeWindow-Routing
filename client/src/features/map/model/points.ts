import type { FeatureCollection, Point } from "geojson";
import { JOB_TYPES, REQUEST_STATUSES } from "@/features/requests/model/dictionaries";
import type { RequestFilters } from "@/features/requests/types";
import type { RequestPoint } from "../types";

/**
 * Same filters as the requests list, applied on the client: the map already holds every request of the region.
 * The filters carry dictionary ids, the points carry keywords.
 */
export function matchesFilters(p: RequestPoint, f: RequestFilters): boolean {
  const statusKeyword = f.statusId == null ? null : REQUEST_STATUSES.find((s) => s.id === f.statusId)?.keyword;
  const jobTypeKeyword = f.jobTypeId == null ? null : JOB_TYPES.find((j) => j.id === f.jobTypeId)?.keyword;
  // part of the address anywhere in it, case-insensitive — as the server searches the list
  const addressPart = f.address?.trim().toLowerCase();
  const crewNamePart = f.crewName?.trim().toLowerCase();
  return (
    (statusKeyword == null || p.status?.keyword === statusKeyword) &&
    (jobTypeKeyword == null || p.jobType.keyword === jobTypeKeyword) &&
    (f.urgent == null || p.urgent === f.urgent) &&
    (!addressPart || p.fullAddress.toLowerCase().includes(addressPart)) &&
    // a request without a crew never matches a crew name, as on the server
    (!crewNamePart || !!p.assignedCrew?.toLowerCase().includes(crewNamePart))
  );
}

export { jobTypeColor } from "@/features/timeline/model/colors";

/** Completed and canceled requests are drawn faded and can be hidden */
export const isClosed = (p: RequestPoint): boolean =>
  p.status?.keyword === "COMPLETED" || p.status?.keyword === "CANCELED";

export interface PointProperties {
  id: number;
  /** resolved colour value — MapLibre can't read CSS variables */
  color: string;
  closed: boolean;
  urgent: boolean;
}

export function toFeatureCollection(
  points: RequestPoint[],
  colorOf: (p: RequestPoint) => string,
): FeatureCollection<Point, PointProperties> {
  return {
    type: "FeatureCollection",
    features: points.map((p) => ({
      type: "Feature",
      id: p.id,
      geometry: { type: "Point", coordinates: [p.longitude, p.latitude] },
      properties: { id: p.id, color: colorOf(p), closed: isClosed(p), urgent: p.urgent },
    })),
  };
}

/** [[west, south], [east, north]], or null for no points */
export function boundsOf(points: RequestPoint[]): [[number, number], [number, number]] | null {
  if (!points.length) return null;
  let west = Infinity, south = Infinity, east = -Infinity, north = -Infinity;
  for (const p of points) {
    west = Math.min(west, p.longitude);
    east = Math.max(east, p.longitude);
    south = Math.min(south, p.latitude);
    north = Math.max(north, p.latitude);
  }
  return [[west, south], [east, north]];
}
