<template>
  <div ref="container" class="h-full w-full"></div>
</template>

<script lang="ts">
import type { RouteLocationRaw } from "vue-router";

export type UiRouteStopStatus = "active" | "done" | "canceled";

/** One stop of the route: a numbered pin that links somewhere */
export interface UiRouteStop {
  /** stable identity of the stop */
  key: string;
  longitude: number;
  latitude: number;
  /** text inside the pin, usually its number in the route */
  label: string;
  /** any CSS colour (CSS variables work: pins are DOM elements) */
  color: string;
  /** border colour, e.g. to flag a stop; white when omitted */
  outlineColor?: string;
  /** where the pin leads; without it the pin is only a mark (e.g. the page's own place) */
  to?: RouteLocationRaw;
  /**
   * how far the stop got, drawn on the pin: under way (a white ring and a play badge), done (faded, a check
   * badge), called off (grey, a cross badge); none: still ahead, the pin as it is
   */
  status?: UiRouteStopStatus;
  ariaLabel: string;
  /** extra data-* attributes on the pin, e.g. for a hover tooltip that looks them up */
  data?: Record<string, string>;
}
</script>

<script setup lang="ts">
import "maplibre-gl/dist/maplibre-gl.css";
import maplibregl, { type ExpressionSpecification, type GeoJSONSource } from "maplibre-gl";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { currentMapStyleUrl } from "@/utils/mapStyle";

// MapLibre map of a route: numbered pins and the route's line, leg by leg, each leg in its own colour —
// along `legs` when given (e.g. the road geometry; no legs, no line), otherwise straight lines joining the
// pins in order. With `startLabel` / `finishLabel`, the line's first and last points get a start / finish
// icon (the label is its tooltip). Arrows along the legs show the direction of travel; `legLabels` puts a
// label (e.g. the distance) halfway along each leg.
// Refits the view whenever the stops or the legs change.

const props = defineProps<{
  /** in route order */
  stops: UiRouteStop[];
  /** the line in legs, in order: each a list of [longitude, latitude] points */
  legs?: [number, number][][];
  /** a label at the middle of each leg, by the legs' order, e.g. its distance; null or missing: none */
  legLabels?: (string | null)[];
  /** start icon at the line's first point, with this tooltip, e.g. "Старт"; none when omitted */
  startLabel?: string;
  /** finish icon at the line's last point, with this tooltip, e.g. "Финиш"; none when omitted */
  finishLabel?: string;
}>();

const ROUTE = "route";
/** the arrows along the route's legs, a symbol layer over the same source */
const ROUTE_ARROWS = "route-arrows";
/** the arrow image's side, in CSS px */
const ARROW_SIZE_PX = 14;
/** drawn at twice the size, for sharp arrows on high-density screens */
const ARROW_PIXEL_RATIO = 2;
const arrowImageName = (colorIndex: number) => `route-arrow-${colorIndex}`;
/**
 * Leg colours, in order, repeating after the last. None may be mistaken for another leg's or for a colour
 * that already means something on the map: every hue keeps ≥25° from the job types the pins carry
 * (installation blue ~224°, local green ~142°, add-on yellow ~45°, emergency red ~347°) and the urgent
 * magenta (~292°), and from the other legs' (teal and cyan are one colour to the eye, so only teal). The
 * few free hues are teal, orange, chartreuse and violet; where they run out, lightness tells two apart
 * (dark brown vs orange, light vs deep violet), plus a neutral slate. Ordered so that consecutive legs
 * differ the most.
 */
const LEG_COLORS = [
  "#0d9488", // teal, ~175°
  "#ea580c", // orange, ~21°
  "#7c3aed", // deep violet, ~262°
  "#84cc16", // chartreuse, ~84°
  "#64748b", // slate, neutral
  "#78350f", // dark brown, ~23° — far darker than the orange
  "#a78bfa", // light violet, ~255° — far lighter than the deep violet
];
/** Central Russia, until there are stops */
const FALLBACK_CENTER: [number, number] = [37.62, 55.75];

const router = useRouter();
const container = ref<HTMLElement | null>(null);
let map: maplibregl.Map | null = null;
let markers: maplibregl.Marker[] = [];
/** the start / finish flags */
let endMarkers: maplibregl.Marker[] = [];
/** the legs' labels (legLabels) */
let legLabelMarkers: maplibregl.Marker[] = [];
/** half a numbered pin (h-6) and a gap: a flag on a pin's spot stands above the pin instead of covering it */
const FLAG_LIFT_PX = 14;
/** how close to a pin (in degrees, ~50 m) a flag's point counts as the pin's spot */
const PIN_SPOT_DEGREES = 0.0005;
/** px between the centres of pins spread apart at one spot: a pin (min-w-6) and a small gap */
const PIN_SPREAD_PX = 26;

const coordsOf = (stop: UiRouteStop): [number, number] => [stop.longitude, stop.latitude];

/**
 * An arrowhead pointing east, in the leg's colour with a white edge so it stands out on its own line.
 * Placed along a line, MapLibre turns it to the line's direction — east becomes the direction of travel.
 */
function createArrowImage(color: string): ImageData {
  const side = ARROW_SIZE_PX * ARROW_PIXEL_RATIO;
  const canvas = document.createElement("canvas");
  canvas.width = side;
  canvas.height = side;
  const context = canvas.getContext("2d")!;
  context.scale(ARROW_PIXEL_RATIO, ARROW_PIXEL_RATIO);

  // a notched arrowhead: tip at the right, notch at the left, inside a 14×14 box with room for the edge
  context.beginPath();
  context.moveTo(3, 2.5);
  context.lineTo(12, 7);
  context.lineTo(3, 11.5);
  context.lineTo(5.5, 7);
  context.closePath();
  context.fillStyle = color;
  context.fill();
  context.lineJoin = "round";
  context.lineWidth = 1.5;
  context.strokeStyle = "#ffffff";
  context.stroke();

  return context.getImageData(0, 0, side, side);
}

interface DrawnLeg {
  points: [number, number][];
  /** from legLabels, by the leg's place among the legs given (before empty ones are dropped) */
  label: string | null;
}

/** the legs to draw: as given, or a straight line between each two consecutive pins; empty legs dropped */
function currentLegs(): DrawnLeg[] {
  const legs = props.legs ?? props.stops.slice(1).map((stop, index) => [coordsOf(props.stops[index]!), coordsOf(stop)]);
  return legs
    .map((points, index) => ({ points, label: props.legLabels?.[index] ?? null }))
    .filter((leg) => leg.points.length >= 2);
}

/**
 * The point halfway along a line, by its length (flat-earth distances: fine within a city) — where its
 * label goes, on the line itself even where it bends
 */
function midpointOf(points: [number, number][]): [number, number] {
  const cosLatitude = Math.cos(((points[0]![1] ?? 0) * Math.PI) / 180);
  const segmentLength = (from: [number, number], to: [number, number]) =>
    Math.hypot((to[0] - from[0]) * cosLatitude, to[1] - from[1]);

  const total = points.slice(1).reduce((sum, point, index) => sum + segmentLength(points[index]!, point), 0);
  let remaining = total / 2;
  for (let index = 1; index < points.length; index++) {
    const from = points[index - 1]!;
    const to = points[index]!;
    const length = segmentLength(from, to);
    if (length >= remaining && length > 0) {
      const share = remaining / length;
      return [from[0] + (to[0] - from[0]) * share, from[1] + (to[1] - from[1]) * share];
    }
    remaining -= length;
  }
  return points[points.length - 1]!;
}

/**
 * A leg's label: a small pill edged in the leg's colour, so it reads as that line's. Hovering it brings its
 * leg forward: the other legs (their lines, arrows and labels) fade out
 */
function createLegLabelElement(text: string, color: string, legIndex: number): HTMLElement {
  const element = document.createElement("span");
  element.textContent = text;
  element.dataset.legIndex = String(legIndex);
  element.className =
    "mono cursor-default rounded-full border-2 bg-surface px-1.5 py-px text-[11px] leading-tight font-semibold whitespace-nowrap text-ink shadow-[0_1px_3px_rgb(0_0_0/0.25)] motion-safe:transition-opacity";
  element.style.borderColor = color;
  element.addEventListener("mouseenter", () => highlightLeg(legIndex));
  element.addEventListener("mouseleave", () => highlightLeg(null));
  return element;
}

/** how visible the other legs stay while one is highlighted */
const FADED_OPACITY = 0.15;
const LINE_OPACITY = 0.95;

/** One leg forward, the rest faded; null: all legs as usual */
function highlightLeg(legIndex: number | null): void {
  if (!map?.getLayer(ROUTE)) return;
  const isHighlighted: ExpressionSpecification = ["==", ["get", "legIndex"], legIndex ?? -1];
  map.setPaintProperty(ROUTE, "line-opacity", legIndex == null ? LINE_OPACITY : ["case", isHighlighted, LINE_OPACITY, FADED_OPACITY]);
  map.setPaintProperty(ROUTE_ARROWS, "icon-opacity", legIndex == null ? 1 : ["case", isHighlighted, 1, FADED_OPACITY]);

  for (const marker of legLabelMarkers) {
    const element = marker.getElement();
    const faded = legIndex != null && element.dataset.legIndex !== String(legIndex);
    element.style.opacity = faded ? "0.35" : "";
  }
}

/**
 * Screen offsets of the pins, [x, y] px: pins at one spot (several requests at one address) would cover each
 * other, so each such group is spread into a row centred on the spot, in route order. The pins keep their
 * coordinates — only where they are drawn moves, the same at any zoom; a pin alone stays on its point.
 */
function pinSpreadOffsets(stops: UiRouteStop[]): [number, number][] {
  // the same spot: equal to ~1 m (5 decimals)
  const spotOf = (stop: UiRouteStop) => `${stop.longitude.toFixed(5)},${stop.latitude.toFixed(5)}`;
  const groups = new Map<string, number[]>();
  stops.forEach((stop, index) => {
    const spot = spotOf(stop);
    groups.set(spot, [...(groups.get(spot) ?? []), index]);
  });

  return stops.map((stop, index) => {
    const group = groups.get(spotOf(stop))!;
    if (group.length < 2) return [0, 0];
    const place = group.indexOf(index);
    return [(place - (group.length - 1) / 2) * PIN_SPREAD_PX, 0];
  });
}

/** a stop status's badge: a small round icon on the pin's top-right, in its colour */
const STATUS_BADGES: Record<UiRouteStopStatus, { color: string; icon: string }> = {
  active: { color: "var(--job-installation)", icon: "M7 4v16l13-8z" },
  done: { color: "var(--job-local)", icon: "M20 6 9 17l-5-5" },
  canceled: { color: "var(--faint)", icon: "M18 6 6 18M6 6l12 12" },
};

/**
 * The pin's look for how far its stop got, as the timeline's bars show it: under way — a white ring inside;
 * done — faded; called off — grey instead of its colour, the number muted. Each with a small badge
 */
function styleStatus(element: HTMLElement, status: UiRouteStopStatus): void {
  if (status === "active") element.style.boxShadow = "inset 0 0 0 2px #ffffff, 0 1px 4px rgb(0 0 0 / 0.3)";
  if (status === "done") element.style.opacity = "0.6";
  if (status === "canceled") {
    element.style.background = "var(--slot-canceled)";
    element.style.color = "var(--muted)";
  }

  const badge = STATUS_BADGES[status];
  const badgeElement = document.createElement("span");
  badgeElement.className =
    "absolute -top-1.5 -end-1.5 grid size-3.5 place-items-center rounded-full border border-white text-white";
  badgeElement.style.background = badge.color;
  badgeElement.setAttribute("aria-hidden", "true");
  // static markup of our own, no user data
  badgeElement.innerHTML = `<svg class="size-2" viewBox="0 0 24 24" fill="${status === "active" ? "currentColor" : "none"}" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"><path d="${badge.icon}" /></svg>`;
  element.append(badgeElement);
}

/** Numbered pin; a real link when it leads somewhere, so middle-click and keyboard work; else a plain mark */
function createMarkerElement(stop: UiRouteStop): HTMLElement {
  const element = document.createElement(stop.to ? "a" : "span");
  element.textContent = stop.label;
  element.setAttribute("aria-label", stop.ariaLabel);
  for (const [name, value] of Object.entries(stop.data ?? {})) element.dataset[name] = value;
  element.className =
    "mono relative grid h-6 min-w-6 place-items-center rounded-full border-2 px-1 text-[11.5px] font-semibold text-white no-underline shadow-[0_1px_4px_rgb(0_0_0/0.3)]";
  element.style.background = stop.color;
  element.style.borderColor = stop.outlineColor ?? "#ffffff";
  if (stop.status) styleStatus(element, stop.status);

  if (stop.to) {
    const route = router.resolve(stop.to);
    (element as HTMLAnchorElement).href = route.href;
    element.classList.add(
      "cursor-pointer",
      "hover:brightness-110",
      "focus-visible:outline-2",
      "focus-visible:outline-offset-2",
      "focus-visible:outline-accent",
    );
    element.addEventListener("click", (event) => {
      if (event.ctrlKey || event.metaKey || event.shiftKey || event.button !== 0) return;
      event.preventDefault();
      void router.push(route);
    });
  } else {
    element.setAttribute("role", "img");
  }
  return element;
}

/** 24×24 icons, drawn in currentColor: "play" for the start, a checkered flag for the finish */
const FLAG_ICONS: Record<"start" | "finish", string> = {
  start: '<path d="M8.5 5.5v13l10-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />',
  finish:
    '<path d="M5 21V4" stroke="currentColor" stroke-width="2" stroke-linecap="round" />' +
    '<rect x="5" y="4" width="14" height="10" fill="none" stroke="currentColor" stroke-width="1.5" />' +
    // 4×4 checks of 3.5×2.5, every other one filled
    '<path fill="currentColor" d="M5 4h3.5v2.5H5zM12 4h3.5v2.5H12zM8.5 6.5H12V9H8.5zM15.5 6.5H19V9h-3.5zM5 9h3.5v2.5H5zM12 9h3.5v2.5H12zM8.5 11.5H12V14H8.5zM15.5 11.5H19V14h-3.5z" />',
};

/**
 * A round icon badge in the page's ink pointing down at its spot, like a map pin: "play" for the start,
 * a checkered flag for the finish. The label is its tooltip and accessible name. Not a link: a
 * click falls through to nothing, and it stands above a pin rather than on it (FLAG_LIFT_PX).
 */
function createFlagElement(label: string, kind: "start" | "finish"): HTMLElement {
  const element = document.createElement("div");
  element.setAttribute("role", "img");
  element.setAttribute("aria-label", label);
  element.title = label;
  element.className = "flex flex-col items-center drop-shadow-[0_1px_3px_rgb(0_0_0/0.35)]";

  // both in the page's ink, the icon inverted, so they read in both themes; the icon tells them apart
  const background = "var(--ink)";

  const badge = document.createElement("span");
  badge.className = "grid size-7 place-items-center rounded-full border-2 border-white";
  badge.style.background = background;
  badge.style.color = "var(--surface)";
  // static markup of our own, no user data
  badge.innerHTML = `<svg class="size-4" viewBox="0 0 24 24" aria-hidden="true">${FLAG_ICONS[kind]}</svg>`;

  // the tip, a small triangle in the badge's colour
  const tip = document.createElement("span");
  tip.className = "-mt-px size-0 border-x-[5px] border-t-[6px] border-x-transparent";
  tip.style.borderTopColor = background;

  element.append(badge, tip);
  return element;
}

function renderEndMarkers(
  currentMap: maplibregl.Map,
  first: [number, number] | undefined,
  last: [number, number] | undefined,
): void {
  endMarkers.forEach((marker) => marker.remove());
  endMarkers = [];

  // the flag's tip at its spot; lifted when a pin is there (the last visit, usually). Road geometry ends
  // on the road near the address, not on it: within ~50 m counts as the pin's spot
  const nearStop = (point: [number, number]) =>
    props.stops.some(
      (stop) => Math.abs(stop.longitude - point[0]) < PIN_SPOT_DEGREES && Math.abs(stop.latitude - point[1]) < PIN_SPOT_DEGREES,
    );

  const addFlag = (label: string, kind: "start" | "finish", point: [number, number]) => {
    endMarkers.push(
      new maplibregl.Marker({
        element: createFlagElement(label, kind),
        anchor: "bottom",
        offset: [0, nearStop(point) ? -FLAG_LIFT_PX : 0],
      })
        .setLngLat(point)
        .addTo(currentMap),
    );
  };

  if (props.startLabel && first) addFlag(props.startLabel, "start", first);
  if (props.finishLabel && last) addFlag(props.finishLabel, "finish", last);
}

function render(): void {
  const currentMap = map;
  if (!currentMap) return;

  markers.forEach((marker) => marker.remove());
  const spreadOffsets = pinSpreadOffsets(props.stops);
  markers = props.stops.map((stop, index) =>
    new maplibregl.Marker({ element: createMarkerElement(stop), offset: spreadOffsets[index] })
      .setLngLat(coordsOf(stop))
      .addTo(currentMap),
  );

  const legs = currentLegs();
  // the flags go to where the line starts and ends: the first leg's first point, the last leg's last one
  const first = legs[0]?.points[0];
  const lastLeg = legs[legs.length - 1]?.points;
  renderEndMarkers(currentMap, first, lastLeg?.[lastLeg.length - 1]);

  currentMap.getSource<GeoJSONSource>(ROUTE)?.setData({
    type: "FeatureCollection",
    features: legs.map((leg, index) => ({
      type: "Feature",
      properties: {
        color: LEG_COLORS[index % LEG_COLORS.length],
        arrow: arrowImageName(index % LEG_COLORS.length),
        // which leg it is: hovering a leg's label brings out the features with its index (highlightLeg)
        legIndex: index,
      },
      geometry: { type: "LineString", coordinates: leg.points },
    })),
  });

  // each leg's label (its distance, say) halfway along it, edged in the leg's colour
  legLabelMarkers.forEach((marker) => marker.remove());
  legLabelMarkers = legs.flatMap((leg, index) =>
    leg.label
      ? [
          new maplibregl.Marker({
            element: createLegLabelElement(leg.label, LEG_COLORS[index % LEG_COLORS.length]!, index),
          })
            .setLngLat(midpointOf(leg.points))
            .addTo(currentMap),
        ]
      : [],
  );
  // a label hovered before the legs changed is gone: nothing stays faded
  highlightLeg(null);

  // the line may leave the pins' box (a detour, the start at the office): fit both
  if (legs.length || props.stops.length) {
    const bounds = new maplibregl.LngLatBounds();
    props.stops.forEach((stop) => bounds.extend(coordsOf(stop)));
    legs.forEach((leg) => leg.points.forEach((point) => bounds.extend(point)));
    currentMap.fitBounds(bounds, { padding: 48, maxZoom: 15, duration: 0 });
  }
}

onMounted(() => {
  if (!container.value) return;
  const firstStop = props.stops[0];
  map = new maplibregl.Map({
    container: container.value,
    style: currentMapStyleUrl(),
    center: firstStop ? coordsOf(firstStop) : FALLBACK_CENTER,
    zoom: 11,
    attributionControl: { compact: true },
  });
  map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-right");

  map.once("load", () => {
    if (!map) return;
    map.addSource(ROUTE, {
      type: "geojson",
      data: { type: "FeatureCollection", features: [] },
    });
    map.addLayer({
      id: ROUTE,
      type: "line",
      source: ROUTE,
      layout: { "line-join": "round", "line-cap": "round" },
      paint: {
        "line-width": ["interpolate", ["linear"], ["zoom"], 10, 2.5, 16, 4],
        // shifted to the right of the direction of travel: a road driven there and back shows as two
        // parallel lines instead of one on top of the other
        "line-offset": ["interpolate", ["linear"], ["zoom"], 10, 1.5, 16, 4],
        // each leg in its own colour (LEG_COLORS)
        "line-color": ["get", "color"],
        "line-opacity": LINE_OPACITY,
      },
    });

    // arrows along each leg, in its colour, turned to the direction of travel
    LEG_COLORS.forEach((color, index) =>
      map!.addImage(arrowImageName(index), createArrowImage(color), { pixelRatio: ARROW_PIXEL_RATIO }),
    );
    map.addLayer({
      id: ROUTE_ARROWS,
      type: "symbol",
      source: ROUTE,
      layout: {
        "symbol-placement": "line",
        // px between arrows along the line
        "symbol-spacing": 90,
        "icon-image": ["get", "arrow"],
        "icon-size": ["interpolate", ["linear"], ["zoom"], 10, 0.8, 16, 1.1],
        // lie flat along the line, turning with it
        "icon-rotation-alignment": "map",
        // on their leg even where they crowd: pins are DOM markers above the map and stay on top anyway
        "icon-allow-overlap": true,
        "icon-ignore-placement": true,
        // symbols ignore line-offset: shift them the same way (to the right of travel, which is down for an
        // arrow pointing east), in icon units — the line's offset over the icon size at those zooms
        "icon-offset": ["interpolate", ["linear"], ["zoom"], 10, ["literal", [0, 1.9]], 16, ["literal", [0, 3.6]]],
      },
    });
    render();
  });
});

onBeforeUnmount(() => {
  markers.forEach((marker) => marker.remove());
  markers = [];
  endMarkers.forEach((marker) => marker.remove());
  endMarkers = [];
  legLabelMarkers.forEach((marker) => marker.remove());
  legLabelMarkers = [];
  map?.remove();
  map = null;
});

watch(
  [() => props.stops, () => props.legs, () => props.legLabels, () => props.startLabel, () => props.finishLabel],
  render,
);
</script>
