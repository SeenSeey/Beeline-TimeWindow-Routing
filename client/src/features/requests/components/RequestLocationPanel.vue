<template>
  <!-- where the request is: on the map by its coordinates, then the address and the region's office -->
  <UiPanel title="Место">
    <div class="h-[420px] border-b border-line">
      <!-- a single pin in the job type's colour, outlined when urgent; not a link — it is this page -->
      <UiRouteMap :stops="stops" :legs="[]" />
    </div>
    <UiDetailList :rows="rows" />
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiDetailList, { type UiDetailRow } from "@/components/ui/UiDetailList.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import UiRouteMap, { type UiRouteStop } from "@/components/ui/UiRouteMap.vue";
import { jobTypeColor } from "../model/colors";
import type { RequestDetailed } from "../types";

const props = defineProps<{
  request: RequestDetailed;
}>();

const stops = computed<UiRouteStop[]>(() => {
  const request = props.request;
  return [
    {
      key: String(request.id),
      longitude: request.longitude,
      latitude: request.latitude,
      label: String(request.id),
      color: jobTypeColor(request.jobType.keyword),
      outlineColor: request.urgent ? "var(--urgent)" : undefined,
      ariaLabel: `Заявка №${request.id}, ${request.fullAddress}`,
    },
  ];
});

const rows = computed<UiDetailRow[]>(() => {
  const request = props.request;
  return [
    { label: "Адрес", value: request.fullAddress },
    { label: "Регион", value: request.region.displayName },
    { label: "Офис региона", value: request.region.officeAddress },
    { label: "Координаты", value: `${request.latitude.toFixed(5)}, ${request.longitude.toFixed(5)}`, mono: true },
  ];
});
</script>
