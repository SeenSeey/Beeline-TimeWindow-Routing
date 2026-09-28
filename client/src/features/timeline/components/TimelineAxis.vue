<template>
  <!-- Preline table head: gray-50 band with small muted labels -->
  <div
    class="sticky top-0 z-[3] grid border-b border-line bg-canvas"
    :style="{ gridTemplateColumns: `${labelWidth}px 1fr` }"
  >
    <!-- corner above the sticky crew column -->
    <div
      class="sticky left-0 z-[2] flex items-center bg-canvas px-5 text-xs font-semibold tracking-wide text-ink uppercase"
      :class="{ 'shadow-[2px_0_4px_rgb(0_0_0/0.06)]': scrolled }"
    >
      Бригада
    </div>
    <div class="relative h-9">
      <span
        v-for="(h, i) in hours"
        :key="h"
        class="mono absolute top-2.5 text-xs text-muted"
        :class="i === 0 ? 'ps-1' : '-translate-x-1/2'"
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
  /** px of the crew column, shared with the rows (TimelineChart measures it) */
  labelWidth: number;
  /** the track is scrolled sideways — the sticky column casts a shadow */
  scrolled: boolean;
}>();
</script>
