<template>
  <div ref="container" class="h-full w-full"></div>
</template>

<script lang="ts">
import type { RouteLocationRaw } from "vue-router";

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
  /** where the pin leads */
  to: RouteLocationRaw;
  ariaLabel: string;
  /** extra data-* attributes on the pin, e.g. for a hover tooltip that looks them up */
  data?: Record<string, string>;
}
</script>

<script setup lang="ts">
import "maplibre-gl/dist/maplibre-gl.css";
import maplibregl, { type GeoJSONSource } from "maplibre-gl";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { currentMapStyleUrl } from "@/utils/mapStyle";

// MapLibre map of a route: numbered pins and the route's line, leg by leg, each leg in its own colour —
// along `legs` when given (e.g. the road geometry; no legs, no line), otherwise straight lines joining the
// pins in order. With `startLabel` / `finishLabel`, the line's first and last points get a start / finish
// icon (the label is its tooltip). Arrows along the legs show the direction of travel.
// Refits the view whenever the stops or the legs change.

const props = defineProps<{
  /** in route order */
  stops: UiRouteStop[];
  /** the line in legs, in order: each a list of [longitude, latitude] points */
  legs?: [number, number][][];
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
/** half a numbered pin (h-6) and a gap: a flag on a pin's spot stands above the pin instead of covering it */
const FLAG_LIFT_PX = 14;
/** how close to a pin (in degrees, ~50 m) a flag's point counts as the pin's spot */
const PIN_SPOT_DEGREES = 0.0005;

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

/** the legs to draw: as given, or a straight line between each two consecutive pins; empty legs dropped */
function currentLegs(): [number, number][][] {
  const legs = props.legs ?? props.stops.slice(1).map((stop, index) => [coordsOf(props.stops[index]!), coordsOf(stop)]);
  return legs.filter((leg) => leg.length >= 2);
}

/** Numbered pin; a real link, so middle-click and keyboard work */
function createMarkerElement(stop: UiRouteStop): HTMLElement {
  const element = document.createElement("a");
  const route = router.resolve(stop.to);
  element.href = route.href;
  element.textContent = stop.label;
  element.setAttribute("aria-label", stop.ariaLabel);
  for (const [name, value] of Object.entries(stop.data ?? {})) element.dataset[name] = value;
  element.className =
    "mono grid h-6 min-w-6 cursor-pointer place-items-center rounded-full border-2 px-1 text-[11.5px] font-semibold text-white no-underline shadow-[0_1px_4px_rgb(0_0_0/0.3)] hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent";
  element.style.background = stop.color;
  element.style.borderColor = stop.outlineColor ?? "#ffffff";
  element.addEventListener("click", (event) => {
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.button !== 0) return;
    event.preventDefault();
    void router.push(route);
  });
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
  markers = props.stops.map((stop) =>
    new maplibregl.Marker({ element: createMarkerElement(stop) }).setLngLat(coordsOf(stop)).addTo(currentMap),
  );

  const legs = currentLegs();
  // the flags go to where the line starts and ends: the first leg's first point, the last leg's last one
  const first = legs[0]?.[0];
  const lastLeg = legs[legs.length - 1];
  renderEndMarkers(currentMap, first, lastLeg?.[lastLeg.length - 1]);

  currentMap.getSource<GeoJSONSource>(ROUTE)?.setData({
    type: "FeatureCollection",
    features: legs.map((leg, index) => ({
      type: "Feature",
      properties: {
        color: LEG_COLORS[index % LEG_COLORS.length],
        arrow: arrowImageName(index % LEG_COLORS.length),
      },
      geometry: { type: "LineString", coordinates: leg },
    })),
  });

  // the line may leave the pins' box (a detour, the start at the office): fit both
  if (legs.length || props.stops.length) {
    const bounds = new maplibregl.LngLatBounds();
    props.stops.forEach((stop) => bounds.extend(coordsOf(stop)));
    legs.forEach((leg) => leg.forEach((point) => bounds.extend(point)));
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
        "line-opacity": 0.95,
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
  map?.remove();
  map = null;
});

watch([() => props.stops, () => props.legs, () => props.startLabel, () => props.finishLabel], render);
</script>
