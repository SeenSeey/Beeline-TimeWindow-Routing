<template>
  <div
    v-if="visit"
    ref="tipEl"
    class="pointer-events-none fixed z-10 max-w-[300px] rounded-md bg-tooltip px-2.5 py-2 text-[12.5px] leading-[1.4] text-tooltip-ink shadow-[0_6px_20px_rgb(0_0_0/0.18)]"
    :style="{ left: x + 'px', top: y + 'px' }"
  >
    <div>
      <b class="mono">#{{ visit.slot.requestId }}</b> · <b>{{ visit.slot.jobType.displayName }}</b
      >{{ visit.urgent ? " · срок сегодня" : "" }}
    </div>
    <div>{{ visit.slot.fullAddress }}</div>
    <div class="mono text-[12px]">
      Окно {{ formatTime(visit.slot.requestTimeWindowStart) }}–{{ formatTime(visit.slot.requestTimeWindowEnd) }} ·
      {{ visit.duration }} мин
    </div>
    <div class="mono text-[12px]">
      Приезд {{ formatTime(visit.arrival) }}
    </div>
    <div class="mono text-[12px]">
      Работа {{ formatTime(visit.slot.startSlotTime) }}–{{ formatTime(visit.slot.endSlotTime) }}
    </div>
    <div>{{ visit.crewName }}</div>
    <div v-if="visit.slot.status">Статус: {{ visit.slot.status.displayName }}</div>
    <div v-if="visit.slot.requiredVehicle">Нужен транспорт: {{ visit.slot.requiredVehicle.displayName }}</div>
    <div v-if="visit.outOfWindow" class="text-red-500">Назначено вне запрошенного окна</div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { formatTime } from "../model/format";
import type { Visit } from "../model/timeline";
import { useBoardStore } from "../stores/board";

// Follows the mouse over any element marked with data-visit="<visitKey>"

const board = useBoardStore();

const tipEl = ref<HTMLElement | null>(null);
const visit = ref<Visit | null>(null);
const x = ref(0);
const y = ref(0);

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
