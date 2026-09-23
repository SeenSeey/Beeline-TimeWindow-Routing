<template>
  <div ref="container" class="h-full w-full"></div>
</template>

<script setup lang="ts">
import "maplibre-gl/dist/maplibre-gl.css";
import maplibregl, { type GeoJSONSource } from "maplibre-gl";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { formatTime } from "../model/format";
import { visitKey, type Visit } from "../model/timeline";

const props = defineProps<{
  /** the crew's visits in route order */
  visits: Visit[];
}>();

const ROUTE = "route";
/** light vector basemap, free and without an API key */
const STYLE_URL = "https://tiles.openfreemap.org/styles/positron";

const router = useRouter();
const container = ref<HTMLElement | null>(null);
let map: maplibregl.Map | null = null;
let markers: maplibregl.Marker[] = [];

const coordsOf = (v: Visit): [number, number] => [v.slot.longitude, v.slot.latitude];

/** "var(--accent)" → its current value; MapLibre paint can't use CSS variables (DOM markers can) */
function resolveColor(value: string): string {
  const name = /^var\((--[\w-]+)\)$/.exec(value)?.[1];
  if (!name || !container.value) return value;
  return getComputedStyle(container.value).getPropertyValue(name).trim() || "#888";
}

/** Numbered pin; a real link, so middle-click and keyboard work. data-visit feeds the board's VisitTooltip. */
function createMarkerElement(v: Visit, index: number): HTMLElement {
  const el = document.createElement("a");
  const route = router.resolve({ name: "request", params: { id: v.slot.requestId } });
  el.href = route.href;
  el.dataset.visit = visitKey(v);
  el.textContent = String(index + 1);
  el.setAttribute(
    "aria-label",
    `${index + 1}. ${formatTime(v.slot.startSlotTime)}–${formatTime(v.slot.endSlotTime)}, ${v.slot.fullAddress}`,
  );
  el.className =
    "mono grid h-6 min-w-6 cursor-pointer place-items-center rounded-full border-2 px-1 text-[11.5px] font-semibold text-white no-underline shadow-[0_1px_4px_rgb(0_0_0/0.3)] hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent";
  el.style.background = v.color;
  el.style.borderColor = v.urgent ? "var(--urgent)" : "#ffffff";
  el.addEventListener("click", (e) => {
    if (e.ctrlKey || e.metaKey || e.shiftKey || e.button !== 0) return;
    e.preventDefault();
    void router.push(route);
  });
  return el;
}

function render(): void {
  if (!map) return;

  markers.forEach((m) => m.remove());
  markers = props.visits.map((v, i) =>
    new maplibregl.Marker({ element: createMarkerElement(v, i) }).setLngLat(coordsOf(v)).addTo(map!),
  );

  map.getSource<GeoJSONSource>(ROUTE)?.setData({
    type: "Feature",
    properties: {},
    geometry: { type: "LineString", coordinates: props.visits.map(coordsOf) },
  });

  if (props.visits.length) {
    const bounds = new maplibregl.LngLatBounds();
    props.visits.forEach((v) => bounds.extend(coordsOf(v)));
    map.fitBounds(bounds, { padding: 48, maxZoom: 15, duration: 0 });
  }
}

onMounted(() => {
  if (!container.value) return;
  map = new maplibregl.Map({
    container: container.value,
    style: STYLE_URL,
    center: props.visits[0] ? coordsOf(props.visits[0]) : [37.62, 55.75],
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
    // straight connections in visit order, not the road geometry
    map.addLayer({
      id: ROUTE,
      type: "line",
      source: ROUTE,
      layout: { "line-join": "round", "line-cap": "round" },
      paint: { "line-color": resolveColor("var(--accent)"), "line-width": 2.5, "line-opacity": 0.8 },
    });
    render();
  });
});

onBeforeUnmount(() => {
  markers.forEach((m) => m.remove());
  markers = [];
  map?.remove();
  map = null;
});

watch(() => props.visits, render);
</script>
