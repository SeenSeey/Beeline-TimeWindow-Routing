<template>
  <div
    class="group grid min-h-[60px] cursor-pointer grid-cols-[230px_1fr] border-b border-line last:border-b-0 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-inset motion-safe:transition-colors"
    :class="selected ? 'bg-accent-soft' : 'hover:bg-canvas'"
    tabindex="0"
    :aria-pressed="selected"
    aria-haspopup="menu"
    @click="emit('select')"
    @keydown.enter.space.prevent="emit('select')"
    @contextmenu="onContextMenu"
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
      <!-- the day's road route, as stored when the schedule was built -->
      <div v-if="routeDistance" class="mono text-xs whitespace-nowrap text-muted" :title="`Маршрут за день: ${routeDistance}`">
        маршрут {{ routeDistance }}
      </div>
      <!-- equipment on hand now: issued with the schedule, minus requests taken into work -->
      <div v-if="equipmentText" class="truncate text-xs text-muted" :title="`Оборудование: ${equipmentText}`">
        {{ equipmentText }}
      </div>
      <!-- edit mode: what the crew can do, to pick where a request fits -->
      <div v-if="edit.active" class="mt-1 flex flex-wrap gap-1" :aria-label="`Навыки: ${skillNames || 'нет'}`">
        <UiBadge v-for="skill in skills" :key="skill.keyword" :color="skillColor(skill.keyword)">
          {{ skill.displayName }}
        </UiBadge>
        <span v-if="!skills.length" class="text-xs text-faint">нет навыков</span>
      </div>
      <UiBadge v-if="edit.active && edit.changedCrewIds.has(crew.id)" color="var(--accent)" class="mt-1 w-fit">
        изменено
      </UiBadge>
      <UiBadge v-if="crew.offShift" color="var(--warning)" class="mt-1 w-fit">снят со смены</UiBadge>
    </div>

    <!-- Track; in edit mode also a drop target for requests dragged from other rows or the unassigned list -->
    <div
      class="relative"
      :class="{ 'bg-accent/10 outline-2 -outline-offset-2 outline-accent outline-dashed': dropHover }"
      @dragover="onDragOver"
      @dragleave="dropHover = false"
      @drop="onDrop"
    >
      <div
        v-for="h in hours"
        :key="h"
        class="absolute top-0 bottom-0 w-px bg-line"
        :style="{ left: scale.left(h * 60) }"
      ></div>

      <!-- the shift; faded out when the crew was taken off it -->
      <div
        class="absolute top-[7px] bottom-[7px] rounded-md"
        :class="[selected ? 'bg-accent/10' : 'bg-shift', { 'opacity-40': crew.offShift }]"
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
        {{ crew.offShift ? "снят со смены" : "без заявок" }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import { skillColor } from "../model/colors";
import { formatDistance, formatTime } from "../model/format";
import { axisMinuteToTime, decodeDragPayload, DRAG_REQUEST_TYPE, indexByDropMinute } from "../model/scheduleDraft";
import type { CrewRow, TimeScale } from "../model/timeline";
import { useBoardStore } from "../stores/board";
import { useCrewShiftStore } from "../stores/crewShift";
import { useScheduleEditStore } from "../stores/scheduleEdit";
import type { JobTypeKeyword } from "../types";
import TimelineWorkBar from "./TimelineWorkBar.vue";
import WaitHatch from "./WaitHatch.vue";

const props = defineProps<{
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

const board = useBoardStore();
const edit = useScheduleEditStore();
const shiftStore = useCrewShiftStore();

/**
 * Right-click on the row (the work bars have their own menu): the crew menu — take it off shift.
 * Not in edit mode: the server's recalculation would clash with the unsaved draft.
 */
function onContextMenu(event: MouseEvent): void {
  if (edit.active) return;
  event.preventDefault();
  // a keyboard-opened context menu reports no pointer position: place it under the crew's label
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  const fromKeyboard = event.clientX === 0 && event.clientY === 0;
  shiftStore.openMenu({
    crewId: props.crew.id,
    x: fromKeyboard ? rect.left : event.clientX,
    y: fromKeyboard ? rect.bottom : event.clientY,
  });
}

// `?? []`: a server from before skills were added to the schedule doesn't send them
const skills = computed(() => props.crew.skills ?? []);
const skillNames = computed(() => skills.value.map((skill) => skill.displayName).join(", "));

// `?? []`: a server from before equipment was added to the schedule doesn't send it
const equipmentText = computed(() =>
  (props.crew.equipment ?? []).map((item) => `${item.displayName} × ${item.quantity}`).join(", "),
);

/** "12,3 км"; empty without a stored route */
const routeDistance = computed(() => {
  const kilometers = props.crew.routeDistanceKilometers;
  return kilometers != null ? formatDistance(kilometers) : "";
});

/** a request is being dragged over this row */
const dropHover = ref(false);

// a crew taken off shift takes no requests, so its row is no drop target
const carriesRequest = (event: DragEvent): boolean =>
  edit.active && !props.crew.offShift && !!event.dataTransfer?.types.includes(DRAG_REQUEST_TYPE);

function onDragOver(event: DragEvent): void {
  if (!carriesRequest(event) || !event.dataTransfer) return;
  event.preventDefault(); // accept the drop
  event.dataTransfer.dropEffect = "move";
  dropHover.value = true;
}

/**
 * Puts the request into this crew's route exactly where it was dropped: the bar's left edge (the cursor
 * minus where the bar was grabbed) marks its start time. The server then checks the new position.
 */
function onDrop(event: DragEvent): void {
  dropHover.value = false;
  if (!carriesRequest(event) || !event.dataTransfer) return;
  event.preventDefault();
  const payload = decodeDragPayload(event.dataTransfer.getData(DRAG_REQUEST_TYPE));
  if (!payload) return;

  const track = (event.currentTarget as HTMLElement).getBoundingClientRect();
  const fraction = Math.min(1, Math.max(0, (event.clientX - payload.grabOffsetPx - track.left) / track.width));
  const startMinute = board.axis.origin + fraction * (board.axis.end - board.axis.origin);
  void edit.place(
    payload.requestId,
    props.crew.id,
    indexByDropMinute(props.crew, payload.requestId, startMinute),
    axisMinuteToTime(startMinute),
  );
}
</script>
