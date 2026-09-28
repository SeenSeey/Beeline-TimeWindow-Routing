<template>
  <UiPanel title="Расписание по бригадам">
    <template #aside>
      <TimelineLegend />
    </template>

    <div class="relative">
      <TimelineChart :region-name="regionName" />

      <!-- recalculating: the old bars stay in view, greyed out under a veil that also keeps them from being
           clicked, dragged or opened while they are about to change; the page refetches until it's done -->
      <div
        v-if="recalculating"
        class="absolute inset-0 z-[5] grid place-items-center rounded-b-lg bg-[color-mix(in_srgb,var(--faint)_35%,transparent)] backdrop-grayscale"
        role="status"
      >
        <span class="inline-flex items-center gap-x-2 rounded-lg border border-line bg-surface px-4 py-2 text-sm font-medium text-ink shadow-md">
          <UiDot color="var(--accent)" pulse />
          Расписание пересчитывается…
        </span>
      </div>
    </div>
  </UiPanel>
</template>

<script setup lang="ts">
import UiDot from "@/components/ui/UiDot.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import TimelineChart from "./TimelineChart.vue";
import TimelineLegend from "./TimelineLegend.vue";

defineProps<{
  regionName: string;
  /** the schedule is being recalculated: its bars are about to change */
  recalculating?: boolean;
}>();
</script>
