<template>
  <UiPanel title="Расписание по бригадам">
    <template #aside>
      <TimelineLegend />
    </template>
    <TimelineChart :region-name="regionName" />
  </UiPanel>
</template>

<script setup lang="ts">
import UiPanel from "@/components/ui/UiPanel.vue";
import TimelineChart from "./TimelineChart.vue";
import TimelineLegend from "./TimelineLegend.vue";

defineProps<{
  regionName: string;
}>();
</script>
