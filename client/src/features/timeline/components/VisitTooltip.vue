<template>
  <div
    v-if="visit"
    ref="tipEl"
    class="pointer-events-none fixed z-30 grid max-w-[300px] gap-0.5 rounded-lg border border-tooltip-border bg-tooltip px-3 py-2 text-xs leading-normal text-tooltip-ink shadow-md"
    :style="{ left: x + 'px', top: y + 'px' }"
  >
    <div>
      <b class="mono">#{{ visit.slot.requestId }}</b> · <b>{{ visit.slot.jobType.displayName }}</b
      >{{ visit.urgent ? " · срок сегодня" : "" }}
    </div>
    <div>{{ visit.slot.fullAddress }}</div>
    <div class="mono">
      Окно {{ formatTime(visit.slot.requestTimeWindowStart) }}–{{ formatTime(visit.slot.requestTimeWindowEnd) }} ·
      {{ visit.duration }} мин
    </div>
    <div class="mono">Приезд {{ formatTime(visit.arrival) }}</div>
    <div class="mono">Работа {{ formatTime(visit.slot.startSlotTime) }}–{{ formatTime(visit.slot.endSlotTime) }}</div>
    <div v-if="visit.slot.requiredEquipment">Нужно оборудование: {{ visit.slot.requiredEquipment }}</div>
    <div v-if="visit.slot.requiredVehicle">Нужен транспорт: {{ visit.slot.requiredVehicle.displayName }}</div>
    <!-- the status as a pill: its colour (as on the status badges elsewhere) and an icon, tinted on the dark tip -->
    <div v-if="visit.slot.status" class="my-0.5">
      <span
        class="inline-flex items-center gap-x-1 rounded-full px-2 py-px font-medium"
        :style="{
          background: `color-mix(in srgb, ${statusColor(visit.slot.status.keyword)} 28%, transparent)`,
        }"
      >
        <svg
          class="size-3 shrink-0"
          :style="{ color: statusColor(visit.slot.status.keyword) }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path :d="STATUS_ICONS[visit.slot.status.keyword] ?? STATUS_ICONS.IDLE" />
        </svg>
        {{ visit.slot.status.displayName }}
      </span>
    </div>
    <div v-if="visit.outOfWindow" class="font-medium text-tooltip-danger">Назначено вне запрошенного окна</div>
    <!-- why a recalculation changed this visit; the bar carries a mark in its corner -->
    <div v-if="visit.slot.alterationReason" class="font-medium text-tooltip-note">
      Изменено: {{ visit.slot.alterationReason.description }}
    </div>
    <!-- edit mode: what the new position breaks -->
    <div v-for="message in violations" :key="message" class="font-medium text-tooltip-danger">⚠ {{ message }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { statusColor } from "@/features/requests/model/colors";
import { formatTime } from "../model/format";
import type { RequestStatusKeyword } from "../types";
import type { Visit } from "../model/timeline";
import { useBoardStore } from "../stores/board";
import { useScheduleEditStore } from "../stores/scheduleEdit";

// Follows the mouse over any element marked with data-visit="<visitKey>"

const board = useBoardStore();
const edit = useScheduleEditStore();

/** 24×24 stroke icons of the request statuses */
const STATUS_ICONS: Record<RequestStatusKeyword, string> = {
  // a dashed circle: nobody's yet
  IDLE: "M10.1 2.2a10 10 0 0 1 3.8 0M17.6 3.7a10 10 0 0 1 2.7 2.7M21.8 10.1a10 10 0 0 1 0 3.8M20.3 17.6a10 10 0 0 1-2.7 2.7M13.9 21.8a10 10 0 0 1-3.8 0M6.4 20.3a10 10 0 0 1-2.7-2.7M2.2 13.9a10 10 0 0 1 0-3.8M3.7 6.4a10 10 0 0 1 2.7-2.7",
  // a person with a check: a crew has it
  ASSIGNED: "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM16 11l2 2 4-4",
  // play: under way
  IN_PROGRESS: "M7 4v16l13-8z",
  // a check: done
  COMPLETED: "M20 6 9 17l-5-5",
  // a cross: called off
  CANCELED: "M18 6 6 18M6 6l12 12",
};

const tipEl = ref<HTMLElement | null>(null);
const visit = ref<Visit | null>(null);
const x = ref(0);
const y = ref(0);

const violations = computed(() =>
  visit.value && edit.active ? (edit.violationsByRequest.get(visit.value.slot.requestId) ?? []) : [],
);

async function onMouseMove(e: MouseEvent): Promise<void> {
  const el = (e.target as Element | null)?.closest?.("[data-visit]");
  visit.value = (el && board.visitsByKey.get(el.getAttribute("data-visit") ?? "")) || null;
  if (!visit.value) return;

  // Measure after render, then flip to stay inside the viewport
  await nextTick();
  const r = tipEl.value?.getBoundingClientRect();
  if (!r) return;
  x.value = e.clientX + 14 + r.width > window.innerWidth - 8 ? e.clientX - r.width - 14 : e.clientX + 14;
  y.value = e.clientY + 14 + r.height > window.innerHeight - 8 ? e.clientY - r.height - 14 : e.clientY + 14;
}

function hide(): void {
  visit.value = null;
}

onMounted(() => {
  document.addEventListener("mousemove", onMouseMove);
  document.addEventListener("scroll", hide, true);
});
onBeforeUnmount(() => {
  document.removeEventListener("mousemove", onMouseMove);
  document.removeEventListener("scroll", hide, true);
});
</script>
