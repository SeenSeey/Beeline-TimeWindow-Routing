<template>
  <!-- start: the office the crew leaves from (the route's first leg); finish: its last visit -->
  <UiRouteMap :stops="stops" :legs="legs" :leg-labels="legLabels" start-label="Старт" finish-label="Финиш" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiRouteMap, { type UiRouteStop, type UiRouteStopStatus } from "@/components/ui/UiRouteMap.vue";
import { routeLegLabels, routeLegs, type LngLat } from "../model/crewRoute";
import { formatTime } from "../model/format";
import { visitKey, type Visit } from "../model/timeline";
import type { CrewRoute } from "../types";

const props = defineProps<{
  /** the crew's visits in route order */
  visits: Visit[];
  /** the crew's stored road route, leg by leg; null while loading or when there is none (schedule
      calculating, crews changed in edit mode) */
  route: CrewRoute | null;
}>();

// Each visit is a numbered pin in its job type colour, outlined when due today; data-visit feeds the
// board's VisitTooltip, the same as on the timeline bars
const stops = computed<UiRouteStop[]>(() =>
  props.visits.map((visit, index) => ({
    key: visitKey(visit),
    longitude: visit.slot.longitude,
    latitude: visit.slot.latitude,
    label: String(index + 1),
    color: visit.color,
    outlineColor: visit.urgent ? "var(--urgent)" : undefined,
    to: { name: "request", params: { id: visit.slot.requestId } },
    ariaLabel: `${index + 1}. ${formatTime(visit.slot.startSlotTime)}–${formatTime(visit.slot.endSlotTime)}, ${visit.slot.fullAddress}${visit.slot.status ? `, ${visit.slot.status.displayName}` : ""}`,
    data: { visit: visitKey(visit) },
    status: stopStatusOf(visit),
  })),
);

/** how far the visit got, the same way the timeline's bar tells it */
function stopStatusOf(visit: Visit): UiRouteStopStatus | undefined {
  const { status, canceledAt, completedAt } = visit.slot;
  if (status?.keyword === "CANCELED" || canceledAt != null) return "canceled";
  if (status?.keyword === "COMPLETED" || completedAt != null) return "done";
  if (status?.keyword === "IN_PROGRESS") return "active";
  return undefined;
}

// the line follows the roads the crew takes, leg by leg from the office through every visit; without a
// stored route there is no line at all — straight segments between the pins would pass for a route that isn't one
const legs = computed<LngLat[][]>(() => (props.route ? routeLegs(props.route) : []));
/** each leg's road distance, at its middle */
const legLabels = computed(() => (props.route ? routeLegLabels(props.route) : []));
</script>
