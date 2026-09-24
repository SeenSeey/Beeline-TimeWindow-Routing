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

// MapLibre map of a route: numbered pins joined in order by straight lines (not the road geometry).
// Refits the view whenever the stops change.

const props = defineProps<{
  /** in route order */
  stops: UiRouteStop[];
}>();

const ROUTE = "route";
/** Central Russia, until there are stops */
const FALLBACK_CENTER: [number, number] = [37.62, 55.75];

const router = useRouter();
const container = ref<HTMLElement | null>(null);
let map: maplibregl.Map | null = null;
let markers: maplibregl.Marker[] = [];

const coordsOf = (stop: UiRouteStop): [number, number] => [stop.longitude, stop.latitude];

/** "var(--accent)" → its current value; MapLibre paint can't use CSS variables (DOM markers can) */
function resolveColor(value: string): string {
  const name = /^var\((--[\w-]+)\)$/.exec(value)?.[1];
  if (!name || !container.value) return value;
  return getComputedStyle(container.value).getPropertyValue(name).trim() || "#888";
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

function render(): void {
  const currentMap = map;
  if (!currentMap) return;

  markers.forEach((marker) => marker.remove());
  markers = props.stops.map((stop) =>
    new maplibregl.Marker({ element: createMarkerElement(stop) }).setLngLat(coordsOf(stop)).addTo(currentMap),
  );

  currentMap.getSource<GeoJSONSource>(ROUTE)?.setData({
    type: "Feature",
    properties: {},
    geometry: { type: "LineString", coordinates: props.stops.map(coordsOf) },
  });

  if (props.stops.length) {
    const bounds = new maplibregl.LngLatBounds();
    props.stops.forEach((stop) => bounds.extend(coordsOf(stop)));
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
      data: { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates: [] } },
    });
    map.addLayer({
      id: ROUTE,
      type: "line",
      source: ROUTE,
      layout: { "line-join": "round", "line-cap": "round" },
      paint: { "line-color": resolveColor("var(--accent)"), "line-width": 3, "line-opacity": 0.95 },
    });
    render();
  });
});

onBeforeUnmount(() => {
  markers.forEach((marker) => marker.remove());
  markers = [];
  map?.remove();
  map = null;
});

watch(() => props.stops, render);
</script>
