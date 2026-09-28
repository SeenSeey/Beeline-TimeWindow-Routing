<template>
  <div ref="wrapper" class="relative h-full w-full">
    <div ref="container" class="h-full w-full"></div>

    <!-- the hovered request, beside the pointer (the selected one has its card instead) -->
    <div
      v-if="hovered"
      ref="tooltip"
      class="pointer-events-none absolute z-[2] grid max-w-[300px] gap-0.5 rounded-lg bg-tooltip px-3 py-2 text-xs leading-normal text-tooltip-ink shadow-md"
      :style="{ left: tooltipPosition.x + 'px', top: tooltipPosition.y + 'px' }"
      role="tooltip"
    >
      <div>
        <b class="mono">№{{ hovered.id }}</b> · <b>{{ hovered.jobType.displayName }}</b>
        <template v-if="hovered.urgent"> · срочная</template>
      </div>
      <div>{{ hovered.fullAddress }}</div>
      <div v-if="hovered.status">Статус: {{ hovered.status.displayName }}</div>
      <div class="mono">
        Окно {{ formatTime(hovered.requestTimeWindowStart) }}–{{ formatTime(hovered.requestTimeWindowEnd) }}
      </div>
      <div class="mono">Срок {{ formatDateTime(hovered.deadline) }}</div>
      <div>{{ hovered.assignedCrew ? `Бригада: ${hovered.assignedCrew}` : "Бригада не назначена" }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import "maplibre-gl/dist/maplibre-gl.css";
import maplibregl, { type GeoJSONSource, type MapLayerMouseEvent } from "maplibre-gl";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { formatDateTime, formatTime } from "@/features/timeline/model/format";
import { currentMapStyleUrl } from "@/utils/mapStyle";
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
/** Central Russia, until the first points arrive */
const FALLBACK_CENTER: [number, number] = [37.62, 55.75];
/** the tooltip's distance from the pointer, and from the map's edges */
const TOOLTIP_GAP_PX = 14;

const wrapper = ref<HTMLElement | null>(null);
const container = ref<HTMLElement | null>(null);
const tooltip = ref<HTMLElement | null>(null);
let map: maplibregl.Map | null = null;

// ── Hover tooltip ──
const hoveredId = ref<number | null>(null);
/** the pointer, in px from the map's top-left corner */
const pointer = ref({ x: 0, y: 0 });
const tooltipPosition = ref({ x: 0, y: 0 });

/** the request under the pointer; none for the selected one, whose card is already open */
const hovered = computed(() =>
  hoveredId.value == null || hoveredId.value === props.selectedId
    ? null
    : (props.points.find((point) => point.id === hoveredId.value) ?? null),
);

/** Beside the pointer, flipped to its other side where the map's edge would cut the tooltip off */
async function placeTooltip(): Promise<void> {
  await nextTick();
  const box = tooltip.value?.getBoundingClientRect();
  const area = wrapper.value?.getBoundingClientRect();
  if (!box || !area) return;
  const { x, y } = pointer.value;
  tooltipPosition.value = {
    x: x + TOOLTIP_GAP_PX + box.width > area.width - TOOLTIP_GAP_PX ? Math.max(0, x - box.width - TOOLTIP_GAP_PX) : x + TOOLTIP_GAP_PX,
    y: y + TOOLTIP_GAP_PX + box.height > area.height - TOOLTIP_GAP_PX ? Math.max(0, y - box.height - TOOLTIP_GAP_PX) : y + TOOLTIP_GAP_PX,
  };
}

function hideTooltip(): void {
  hoveredId.value = null;
}

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
    paint: { "circle-radius": 14, "circle-color": resolveColor("var(--accent)"), "circle-opacity": 0.35 },
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
    style: currentMapStyleUrl(),
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
  // the topmost point under the pointer, with the pointer's place for the tooltip
  map.on("mousemove", "requests", (e: MapLayerMouseEvent) => {
    const feature = e.features?.[0];
    if (!feature) return;
    hoveredId.value = Number(feature.properties.id);
    pointer.value = { x: e.point.x, y: e.point.y };
    void placeTooltip();
  });
  map.on("mouseleave", "requests", (e: MapLayerMouseEvent) => {
    e.target.getCanvas().style.cursor = "";
    hideTooltip();
  });
  // dragging or zooming moves the point from under the tooltip
  map.on("movestart", hideTooltip);
});

onBeforeUnmount(() => {
  map?.remove();
  map = null;
});

watch(() => props.points, () => {
  updateData();
  // the hovered request may be filtered out or gone
  hideTooltip();
});
watch(() => props.selectedId, updateSelection);
watch(
  () => props.fitKey,
  () => fitToPoints(true),
);
</script>
