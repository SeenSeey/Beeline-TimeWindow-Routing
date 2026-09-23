<template>
  <div class="sticky top-0 z-[3] grid grid-cols-[230px_1fr] border-b border-line bg-surface">
    <!-- corner above the sticky crew column -->
    <div class="sticky left-0 z-[2] bg-surface" :class="{ 'shadow-[2px_0_4px_rgb(0_0_0/0.08)]': scrolled }"></div>
    <div class="relative h-7">
      <span
        v-for="(h, i) in hours"
        :key="h"
        class="mono absolute top-2 text-[11px] text-faint"
        :class="i === 0 ? 'pl-1' : '-translate-x-1/2'"
        :style="{ left: scale.left(h * 60) }"
      >
        {{ formatHour(h) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatHour } from "../model/format";
import type { TimeScale } from "../model/timeline";

defineProps<{
  hours: number[];
  scale: TimeScale;
  /** the track is scrolled sideways — the sticky column casts a shadow */
  scrolled: boolean;
}>();
</script>
