<template>
  <div
    v-if="edit.menu && target"
    ref="menuEl"
    class="fixed z-40 grid w-72 gap-1 rounded-lg border border-line bg-surface p-2 text-sm shadow-lg"
    :style="{ left: position.left + 'px', top: position.top + 'px' }"
    role="menu"
    :aria-label="`Действия с заявкой #${target.requestId}`"
  >
    <div class="grid gap-0.5 px-2 pt-1 pb-2">
      <span class="mono font-semibold text-ink">Заявка #{{ target.requestId }}</span>
      <span class="truncate text-xs text-muted" :title="target.address">{{ target.address }}</span>
      <span class="text-xs text-muted">
        {{ target.crew ? target.crew.name : "не в расписании" }} · окно
        <span class="mono">{{ formatTime(target.windowStart) }}–{{ formatTime(target.windowEnd) }}</span>
      </span>
    </div>

    <ul v-if="targetViolations.length" class="m-0 grid list-disc gap-0.5 rounded-md bg-danger/10 py-1.5 ps-6 pe-2 text-xs text-danger">
      <li v-for="message in targetViolations" :key="message">{{ message }}</li>
    </ul>

    <button
      v-if="target.crew"
      type="button"
      role="menuitem"
      class="flex w-full cursor-pointer items-center gap-x-2 rounded-md px-2 py-1.5 text-start text-danger hover:bg-danger/10 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      @click="edit.remove(target.requestId)"
    >
      Убрать из расписания
    </button>

    <div class="px-2 pt-2 pb-1 text-xs text-muted">
      {{ target.crew ? "Переместить в маршрут бригады" : "Поставить в маршрут бригады" }}
    </div>
    <div class="grid max-h-56 gap-0.5 overflow-y-auto">
      <button
        v-for="crew in otherCrews"
        :key="crew.id"
        type="button"
        role="menuitem"
        class="flex w-full cursor-pointer items-center justify-between gap-x-2 rounded-md px-2 py-1.5 text-start text-ink hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
        @click="placeInto(crew)"
      >
        <span class="truncate">{{ crew.name }}</span>
        <span class="mono shrink-0 text-xs text-muted">{{ crew.requests.length }}</span>
      </button>
      <span v-if="!otherCrews.length" class="px-2 py-1.5 text-xs text-faint">Других бригад нет</span>
    </div>

    <div class="mt-1 border-t border-line pt-1">
      <RouterLink
        :to="{ name: 'request', params: { id: target.requestId } }"
        role="menuitem"
        class="block rounded-md px-2 py-1.5 text-accent no-underline hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        Открыть заявку
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { formatTime } from "../model/format";
import { indexByWindow } from "../model/scheduleDraft";
import type { CrewRow } from "../model/timeline";
import { useBoardStore } from "../stores/board";
import { useScheduleEditStore } from "../stores/scheduleEdit";

// Edit-mode actions for one request: remove it from the schedule, put it into a crew's route (placed by
// its time window) or open it. Positioned where it was opened; closes on Escape or a click outside.

const MENU_WIDTH = 288;
const VIEWPORT_MARGIN = 8;

const board = useBoardStore();
const edit = useScheduleEditStore();
const menuEl = ref<HTMLElement | null>(null);

/** the request the menu is about, wherever it is: on a crew's route or in the unassigned list */
const target = computed(() => {
  const requestId = edit.menu?.requestId;
  if (requestId == null) return null;

  for (const crew of board.crews) {
    const visit = crew.requests.find((v) => v.slot.requestId === requestId);
    if (visit) {
      const { fullAddress, requestTimeWindowStart, requestTimeWindowEnd } = visit.slot;
      return { requestId, crew, address: fullAddress, windowStart: requestTimeWindowStart, windowEnd: requestTimeWindowEnd };
    }
  }
  const request = board.unassigned.find((r) => r.id === requestId);
  if (!request) return null;
  return {
    requestId,
    crew: null,
    address: request.fullAddress,
    windowStart: request.requestTimeWindowStart,
    windowEnd: request.requestTimeWindowEnd,
  };
});

/** what the edit broke for this request, if anything */
const targetViolations = computed(() =>
  target.value ? (edit.violationsByRequest.get(target.value.requestId) ?? []) : [],
);

const otherCrews = computed(() => board.crews.filter((crew) => crew.id !== target.value?.crew?.id));

function placeInto(crew: CrewRow): void {
  const current = target.value;
  if (!current) return;
  // no drop point here: placed by its window, as early as possible
  void edit.place(current.requestId, crew.id, indexByWindow(crew, current.requestId, current.windowStart), null);
}

// keep the menu inside the viewport; the height is measured once it is rendered
const menuHeight = ref(0);
const position = computed(() => {
  const x = edit.menu?.x ?? 0;
  const y = edit.menu?.y ?? 0;
  const maxLeft = window.innerWidth - MENU_WIDTH - VIEWPORT_MARGIN;
  const maxTop = window.innerHeight - menuHeight.value - VIEWPORT_MARGIN;
  return { left: Math.max(VIEWPORT_MARGIN, Math.min(x, maxLeft)), top: Math.max(VIEWPORT_MARGIN, Math.min(y, maxTop)) };
});

watch(
  () => edit.menu,
  async (menu) => {
    if (!menu) return;
    await nextTick();
    menuHeight.value = menuEl.value?.offsetHeight ?? 0;
    menuEl.value?.querySelector<HTMLElement>("[role=menuitem]")?.focus();
  },
);

function onPointerDown(event: PointerEvent): void {
  if (edit.menu && menuEl.value && !menuEl.value.contains(event.target as Node)) edit.closeMenu();
}

function onKeyDown(event: KeyboardEvent): void {
  if (event.key === "Escape" && edit.menu) edit.closeMenu();
}

onMounted(() => {
  // capture: runs before the click that may open a menu elsewhere
  document.addEventListener("pointerdown", onPointerDown, true);
  document.addEventListener("keydown", onKeyDown);
});
onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onPointerDown, true);
  document.removeEventListener("keydown", onKeyDown);
});
</script>
