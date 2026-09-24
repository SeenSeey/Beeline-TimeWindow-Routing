<template>
  <div
    class="group grid min-h-[60px] cursor-pointer grid-cols-[230px_1fr] border-b border-line last:border-b-0 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-inset motion-safe:transition-colors"
    :class="selected ? 'bg-accent-soft' : 'hover:bg-canvas'"
    tabindex="0"
    :aria-pressed="selected"
    @click="emit('select')"
    @keydown.enter.space.prevent="emit('select')"
  >
    <!-- Sticky crew label: opaque background mirrors the row's hover/selected state -->
    <div
      class="sticky left-0 z-[2] grid min-w-0 content-center py-1.5 ps-5 pe-3 motion-safe:transition-colors"
      :class="[
        selected ? 'bg-accent-soft' : 'bg-surface group-hover:bg-canvas',
        { 'shadow-[2px_0_4px_rgb(0_0_0/0.06)]': scrolled },
      ]"
    >
      <!-- the name opens the crew's page; stop: the click/Enter must not reach the row, which toggles
           selection and prevents Enter's default -->
      <RouterLink
        :to="{ name: 'crew', params: { id: crew.id } }"
        class="w-fit max-w-full truncate rounded-sm text-sm font-medium no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
        :class="selected ? 'text-accent' : 'text-ink hover:text-accent'"
        :title="`Открыть бригаду ${crew.name}`"
        @click.stop
        @keydown.enter.stop
      >
        {{ crew.name.replace(/^Бригада\s+/, "") }}
      </RouterLink>
      <div v-if="crew.vehicle" class="truncate text-xs text-muted">{{ crew.vehicle.displayName }}</div>
      <div class="mono text-xs whitespace-nowrap text-muted">
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
        class="absolute top-[7px] bottom-[7px] rounded-md"
        :class="selected ? 'bg-accent/10' : 'bg-shift'"
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
          class="absolute top-[11px] bottom-[11px] rounded-sm bg-line-strong"
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
        class="absolute top-2 ps-2 text-xs text-faint"
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
