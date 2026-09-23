<template>
  <div ref="container" class="h-full w-full"></div>
</template>

<script setup lang="ts">
import "maplibre-gl/dist/maplibre-gl.css";
import maplibregl, { type GeoJSONSource, type MapLayerMouseEvent } from "maplibre-gl";
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { boundsOf, jobTypeColor, toFeatureCollection } from "../model/points";
import type { RequestPoint } from "../types";

const props = defineProps<{
  points: RequestPoint[];
  selectedId: number | null;
  /** changes when a new region's points arrive; the map then fits them */
  fitKey: string | number | null;
}>();

const emit = defineEmits<{
  select: [id: number | null];
}>();

const SOURCE = "requests";
/** light vector basemap, free and without an API key */
const STYLE_URL = "https://tiles.openfreemap.org/styles/positron";
/** Central Russia, until the first points arrive */
const FALLBACK_CENTER: [number, number] = [37.62, 55.75];

const container = ref<HTMLElement | null>(null);
let map: maplibregl.Map | null = null;

/** "var(--job-local)" → its current value on the page; MapLibre paint can't use CSS variables */
function resolveColor(value: string): string {
  const name = /^var\((--[\w-]+)\)$/.exec(value)?.[1];
  if (!name || !container.value) return value;
  return getComputedStyle(container.value).getPropertyValue(name).trim() || "#888";
}

function updateData(): void {
  map
    ?.getSource<GeoJSONSource>(SOURCE)
    ?.setData(toFeatureCollection(props.points, (p) => resolveColor(jobTypeColor(p.jobType.keyword))));
}

function updateSelection(): void {
  if (map?.getLayer("requests-selected")) {
    map.setFilter("requests-selected", ["==", ["get", "id"], props.selectedId ?? -1]);
  }
}

function fitToPoints(animate: boolean): void {
  const bounds = boundsOf(props.points);
  if (!map || !bounds) return;
  map.fitBounds(bounds, { padding: 60, maxZoom: 15, duration: animate ? 600 : 0 });
}

function addLayers(): void {
  if (!map) return;
  map.addSource(SOURCE, { type: "geojson", data: { type: "FeatureCollection", features: [] } });

  // selection halo under the markers
  map.addLayer({
    id: "requests-selected",
    type: "circle",
    source: SOURCE,
    filter: ["==", ["get", "id"], -1],
    paint: { "circle-radius": 14, "circle-color": "#17202b", "circle-opacity": 0.2 },
  });

  map.addLayer({
    id: "requests",
    type: "circle",
    source: SOURCE,
    paint: {
      "circle-radius": ["interpolate", ["linear"], ["zoom"], 8, 4, 13, 7, 16, 9],
      "circle-color": ["get", "color"],
      "circle-opacity": ["case", ["get", "closed"], 0.35, 0.95],
      "circle-stroke-color": ["case", ["get", "urgent"], resolveColor("var(--urgent)"), "#ffffff"],
      "circle-stroke-width": ["case", ["get", "urgent"], 2.5, 1.5],
      "circle-stroke-opacity": ["case", ["get", "closed"], 0.5, 1],
    },
  });

  updateData();
  updateSelection();
  fitToPoints(false);
}

onMounted(() => {
  if (!container.value) return;
  map = new maplibregl.Map({
    container: container.value,
    style: STYLE_URL,
    center: FALLBACK_CENTER,
    zoom: 9,
    attributionControl: { compact: true },
  });
  map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-right");

  map.once("load", addLayers);

  map.on("click", (e) => {
    if (!map?.getLayer("requests")) return;
    const hit = map.queryRenderedFeatures(e.point, { layers: ["requests"] })[0];
    emit("select", hit ? Number(hit.properties.id) : null);
  });
  map.on("mouseenter", "requests", (e: MapLayerMouseEvent) => {
    e.target.getCanvas().style.cursor = "pointer";
  });
  map.on("mouseleave", "requests", (e: MapLayerMouseEvent) => {
    e.target.getCanvas().style.cursor = "";
  });
});

onBeforeUnmount(() => {
  map?.remove();
  map = null;
});

watch(() => props.points, updateData);
watch(() => props.selectedId, updateSelection);
watch(
  () => props.fitKey,
  () => fitToPoints(true),
);
</script>
