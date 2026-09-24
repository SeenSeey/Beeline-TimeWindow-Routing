<template>
  <UiRouteMap :stops="stops" />
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiRouteMap, { type UiRouteStop } from "@/components/ui/UiRouteMap.vue";
import { formatTime } from "../model/format";
import { visitKey, type Visit } from "../model/timeline";

const props = defineProps<{
  /** the crew's visits in route order */
  visits: Visit[];
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
    ariaLabel: `${index + 1}. ${formatTime(visit.slot.startSlotTime)}–${formatTime(visit.slot.endSlotTime)}, ${visit.slot.fullAddress}`,
    data: { visit: visitKey(visit) },
  })),
);
</script>
