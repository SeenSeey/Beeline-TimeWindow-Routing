<template>
  <!-- list tile with a square status icon in the job type colour; in edit mode it can be dragged onto a
       crew row, or put into a route from its menu -->
  <li
    class="grid grid-cols-[28px_1fr_auto] items-start gap-x-3 rounded-md bg-shift px-3 py-2.5 hover:bg-line"
    :class="{ 'cursor-grab active:cursor-grabbing': edit.active, 'opacity-40': dragging }"
    :draggable="edit.active ? 'true' : undefined"
    @dragstart="onDragStart"
    @dragend="dragging = false"
  >
    <span class="inline-flex size-7 items-center justify-center rounded-md bg-current/15" :style="{ color }">
      <UiDot />
    </span>

    <div class="grid min-w-0 gap-1.5">
      <RouterLink
        :to="{ name: 'request', params: { id: request.id } }"
        class="block truncate rounded-sm text-sm font-medium text-ink no-underline hover:text-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
        :title="request.fullAddress"
      >
        {{ request.fullAddress }}
      </RouterLink>

      <div class="flex flex-wrap gap-1.5">
        <UiBadge :color="color">{{ request.jobType.displayName }}</UiBadge>
        <UiBadge v-if="request.urgent" color="var(--unassigned)">срочная</UiBadge>
        <UiBadge v-if="dueToday" color="var(--urgent)">срок сегодня</UiBadge>
        <UiBadge v-if="request.status">{{ request.status.displayName }}</UiBadge>
      </div>

      <!-- why the request is out of the schedule, with the same mark as a changed bar on the timeline -->
      <p
        v-if="request.alterationReason"
        class="m-0 flex items-start gap-x-1.5 rounded-md border border-line bg-surface px-2 py-1.5 text-xs text-ink"
      >
        <svg
          class="mt-px size-3.5 shrink-0 text-[var(--job-addon)]"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M21 12a9 9 0 0 1-15.5 6.2L3 16m0 5v-5h5M3 12a9 9 0 0 1 15.5-6.2L21 8m0-5v5h-5" />
        </svg>
        <span><span class="text-muted">Причина:</span> {{ request.alterationReason.description }}</span>
      </p>

      <dl class="m-0 grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 text-xs">
        <template v-for="d in details" :key="d.label">
          <dt class="text-muted">{{ d.label }}</dt>
          <dd class="m-0 text-ink" :class="{ mono: d.mono }">{{ d.value }}</dd>
        </template>
      </dl>
    </div>

    <div class="mono grid justify-items-end gap-1.5 text-end text-xs whitespace-nowrap text-ink">
      <span>
        №{{ request.id }}
        <span class="block text-muted">создана {{ formatDateTime(request.createdAt) }}</span>
      </span>
      <UiButton v-if="edit.active" aria-haspopup="menu" :disabled="!!edit.busy" @click="openMenu">
        В расписание…
      </UiButton>
    </div>
  </li>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import { jobTypeColor } from "../model/colors";
import { formatDateTime, formatTime, isOnDate } from "../model/format";
import { DRAG_REQUEST_TYPE, encodeDragPayload } from "../model/scheduleDraft";
import { useScheduleEditStore } from "../stores/scheduleEdit";
import type { UnassignedRequest } from "../types";

const props = defineProps<{
  request: UnassignedRequest;
  /** schedule's "YYYY-MM-DD", to tell deadlines due today */
  targetDate: string;
}>();

const edit = useScheduleEditStore();
const dragging = ref(false);

function onDragStart(event: DragEvent): void {
  if (!edit.active || !event.dataTransfer) return;
  // from the list the cursor marks the start: there is no bar to grab
  event.dataTransfer.setData(DRAG_REQUEST_TYPE, encodeDragPayload({ requestId: props.request.id, grabOffsetPx: 0 }));
  event.dataTransfer.effectAllowed = "move";
  edit.closeMenu();
  dragging.value = true;
}

function openMenu(event: MouseEvent): void {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
  edit.openMenu({ requestId: props.request.id, x: rect.left, y: rect.bottom });
}

const color = computed(() => jobTypeColor(props.request.jobType.keyword));
const dueToday = computed(() => isOnDate(props.request.deadline, props.targetDate));

/** Label/value rows; optional ones only when the server sent something */
const details = computed(() => {
  const r = props.request;
  const rows: { label: string; value: string; mono?: boolean }[] = [
    {
      label: "Окно",
      value: `${formatTime(r.requestTimeWindowStart)}–${formatTime(r.requestTimeWindowEnd)}`,
      mono: true,
    },
    {
      label: "Срок",
      value: dueToday.value ? `сегодня ${r.deadline.slice(11, 16)}` : formatDateTime(r.deadline),
      mono: true,
    },
    { label: "Работа", value: `${r.jobTimeSpanMinutes} мин · дорога до ${r.maxTravelTimeSpanMinutes} мин` },
  ];
  if (r.requiredSkills.length) {
    rows.push({ label: "Навыки", value: r.requiredSkills.map((s) => s.displayName).join(", ") });
  }
  if (r.equipment.length) rows.push({ label: "Оборудование", value: r.equipment.join(", ") });
  if (r.requiredVehicle) rows.push({ label: "Транспорт", value: r.requiredVehicle.displayName });
  if (r.assignedCrew) {
    const at = r.assignedAt ? ` · назначена ${formatDateTime(r.assignedAt)}` : "";
    rows.push({ label: "Бригада", value: r.assignedCrew + at });
  }
  rows.push({ label: "Координаты", value: `${r.latitude.toFixed(5)}, ${r.longitude.toFixed(5)}`, mono: true });
  return rows;
});
</script>
