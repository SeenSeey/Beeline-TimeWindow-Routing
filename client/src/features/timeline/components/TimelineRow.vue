<template>
  <div
    class="group grid min-h-[60px] cursor-pointer grid-cols-[230px_1fr] rounded focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-safe:transition-colors motion-safe:duration-[120ms]"
    :class="selected ? 'bg-shift' : 'hover:bg-[color-mix(in_srgb,var(--shift)_60%,transparent)]'"
    tabindex="0"
    :aria-pressed="selected"
    @click="emit('select')"
    @keydown.enter.space.prevent="emit('select')"
  >
    <!-- Sticky crew label: opaque background mirrors the row's hover/selected state -->
    <div
      class="sticky left-0 z-[2] grid min-w-0 content-center rounded-l py-[5px] pr-3 pl-4 motion-safe:transition-colors motion-safe:duration-[120ms]"
      :class="[
        selected ? 'bg-shift' : 'bg-surface group-hover:bg-[color-mix(in_srgb,var(--shift)_60%,var(--surface))]',
        { 'shadow-[2px_0_4px_rgb(0_0_0/0.08)]': scrolled },
      ]"
    >
      <div class="truncate font-medium">{{ crew.name.replace(/^Бригада\s+/, "") }}</div>
      <div v-if="crew.vehicle" class="truncate text-[11.5px] text-muted">{{ crew.vehicle.displayName }}</div>
      <div class="mono text-[11.5px] whitespace-nowrap text-muted">
        {{ formatTime(crew.shiftStartTime) }}–{{ formatTime(crew.shiftEndTime) }} · {{ crew.requests.length }} заявок
      </div>
    </div>

    <!-- Track -->
    <div class="relative">
      <div
        v-for="h in hours"
        :key="h"
        class="absolute top-0 bottom-0 w-px bg-line"
        :style="{ left: scale.left(h * 60) }"
      ></div>

      <div
        class="absolute top-[7px] bottom-[7px] rounded-[3px]"
        :class="selected ? 'bg-[color-mix(in_srgb,var(--line-strong)_45%,var(--shift))]' : 'bg-shift'"
        :style="{ left: scale.left(crew.shift[0]), width: scale.width(crew.shift[0], crew.shift[1]) }"
      ></div>

      <template v-for="seg in crew.segments" :key="seg.key">
        <div
          v-if="seg.kind === 'travel'"
          class="absolute top-1/2 -mt-px h-0.5 bg-faint opacity-70"
          :style="{ left: scale.left(seg.from), width: scale.width(seg.from, seg.to) }"
        ></div>
        <WaitHatch
          v-else-if="seg.kind === 'wait'"
          class="absolute top-3 bottom-3"
          :style="scale.snapped(seg.from, seg.to)"
        />
        <div
          v-else-if="seg.kind === 'break'"
          class="absolute top-[11px] bottom-[11px] rounded-[3px] bg-line-strong"
          :style="{ left: scale.left(seg.from), width: scale.width(seg.from, seg.to) }"
        ></div>
        <TimelineWorkBar
          v-else
          :visit="seg.visit"
          :from="seg.from"
          :to="seg.to"
          :scale="scale"
          :dimmed="!!jobTypeFilter && seg.visit.slot.jobType.keyword !== jobTypeFilter"
        />
      </template>

      <span
        v-if="!crew.requests.length"
        class="absolute top-2 pl-2 text-[12px] text-faint"
        :style="{ left: scale.left(crew.shift[0]) }"
      >
        без заявок
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatTime } from "../model/format";
import type { CrewRow, TimeScale } from "../model/timeline";
import type { JobTypeKeyword } from "../types";
import TimelineWorkBar from "./TimelineWorkBar.vue";
import WaitHatch from "./WaitHatch.vue";

defineProps<{
  crew: CrewRow;
  hours: number[];
  scale: TimeScale;
  selected: boolean;
  /** the track is scrolled sideways — the sticky column casts a shadow */
  scrolled: boolean;
  /** active job type keyword filter; other job bars are dimmed */
  jobTypeFilter?: JobTypeKeyword | "";
}>();

const emit = defineEmits<{
  select: [];
}>();
</script>
