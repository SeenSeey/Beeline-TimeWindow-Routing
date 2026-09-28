<template>
  <div
    v-if="visit"
    ref="tipEl"
    class="pointer-events-none fixed z-30 grid max-w-[300px] gap-0.5 rounded-lg bg-tooltip px-3 py-2 text-xs leading-normal text-tooltip-ink shadow-md"
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
    <div>{{ visit.crewName }}</div>
    <div v-if="visit.slot.status">Статус: {{ visit.slot.status.displayName }}</div>
    <div v-if="visit.slot.requiredVehicle">Нужен транспорт: {{ visit.slot.requiredVehicle.displayName }}</div>
    <div v-if="visit.slot.requiredEquipment">Нужно оборудование: {{ visit.slot.requiredEquipment }}</div>
    <div v-if="visit.outOfWindow" class="font-medium text-red-400">Назначено вне запрошенного окна</div>
    <!-- why a recalculation changed this visit; the bar carries a mark in its corner -->
    <div v-if="visit.slot.alterationReason" class="font-medium text-amber-300">
      Изменено: {{ visit.slot.alterationReason.description }}
    </div>
    <!-- edit mode: what the new position breaks -->
    <div v-for="message in violations" :key="message" class="font-medium text-red-400">⚠ {{ message }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { formatTime } from "../model/format";
import type { Visit } from "../model/timeline";
import { useBoardStore } from "../stores/board";
import { useScheduleEditStore } from "../stores/scheduleEdit";

// Follows the mouse over any element marked with data-visit="<visitKey>"

const board = useBoardStore();
const edit = useScheduleEditStore();

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
