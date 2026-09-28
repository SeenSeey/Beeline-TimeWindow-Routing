import type { CrewRoute } from "../types";
import { formatDistance } from "./format";

/** [longitude, latitude], as MapLibre takes coordinates */
export type LngLat = [number, number];

const POLYLINE_FACTOR = 1e5;

/**
 * Encoded polyline (Google's algorithm, precision 5, latitude first — as the server's PolylineEncoder
 * writes it) → points in order
 */
export function decodePolyline(encoded: string): LngLat[] {
  const points: LngLat[] = [];
  let index = 0;
  let latitude = 0;
  let longitude = 0;

  // one coordinate: 5-bit chunks from the lowest, each but the last flagged with 0x20; the sign in the lowest bit
  const readValue = (): number => {
    let result = 0;
    let shift = 0;
    let chunk: number;
    do {
      chunk = encoded.charCodeAt(index++) - 63;
      result |= (chunk & 0x1f) << shift;
      shift += 5;
    } while (chunk >= 0x20);
    return result & 1 ? ~(result >> 1) : result >> 1;
  };

  while (index < encoded.length) {
    latitude += readValue();
    longitude += readValue();
    points.push([longitude / POLYLINE_FACTOR, latitude / POLYLINE_FACTOR]);
  }
  return points;
}

/**
 * The route's legs as lines, in order: from the office to the first visit, then visit to visit. A leg
 * between two requests at one address has no points
 */
export function routeLegs(route: CrewRoute): LngLat[][] {
  return route.legs.map((leg) => decodePolyline(leg.polyline));
}

/** Each leg's road distance, "3,2 км", by the same order as routeLegs — for the labels on the map */
export function routeLegLabels(route: CrewRoute): string[] {
  return route.legs.map((leg) => formatDistance(leg.distanceKilometers));
}
