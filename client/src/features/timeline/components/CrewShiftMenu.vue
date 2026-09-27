<template>
  <div
    v-if="shiftStore.menu && crew"
    ref="menuEl"
    class="fixed z-40 grid w-64 gap-1 rounded-lg border border-line bg-surface p-2 text-sm shadow-lg"
    :style="{ left: position.left + 'px', top: position.top + 'px' }"
    role="menu"
    :aria-label="`Действия с бригадой ${crew.name}`"
  >
    <div class="grid gap-0.5 px-2 pt-1 pb-2">
      <span class="truncate font-semibold text-ink" :title="crew.name">{{ crew.name }}</span>
      <span class="text-xs text-muted">
        смена <span class="mono">{{ formatTime(crew.shiftStartTime) }}–{{ formatTime(crew.shiftEndTime) }}</span>
        · {{ crew.requests.length }} заявок
      </span>
    </div>

    <span v-if="crew.offShift" class="px-2 py-1.5 text-xs text-faint">Бригада уже снята со смены</span>

    <!-- the crew's requests get redistributed and it can't be undone here, so it asks once more -->
    <div v-else-if="confirming" class="grid gap-2 rounded-md bg-danger/10 p-2" role="alertdialog">
      <span class="text-sm font-medium text-danger">Снять бригаду со смены? Её заявки будут перераспределены.</span>
      <div class="flex gap-2">
        <button
          type="button"
          role="menuitem"
          class="inline-flex h-8 flex-1 cursor-pointer items-center justify-center rounded-md bg-danger px-2 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-danger/50"
          @click="confirm"
        >
          Да, снять
        </button>
        <button
          type="button"
          role="menuitem"
          class="inline-flex h-8 flex-1 cursor-pointer items-center justify-center rounded-md border border-line bg-surface px-2 text-sm text-ink hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
          @click="confirming = false"
        >
          Нет
        </button>
      </div>
    </div>

    <button
      v-else
      type="button"
      role="menuitem"
      class="flex w-full cursor-pointer items-center gap-x-2 rounded-md px-2 py-1.5 text-start text-danger hover:bg-danger/10 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
      :disabled="shiftStore.pending"
      @click="confirming = true"
    >
      <UiDot v-if="shiftStore.pending" size="sm" pulse />
      <UiDot v-else size="sm" color="var(--unassigned)" />
      {{ shiftStore.pending ? "Снимаем…" : "Снять со смены" }}
    </button>

    <p v-if="shiftStore.error" class="m-0 rounded-md bg-danger/10 px-2 py-1.5 text-xs text-danger" role="alert">
      {{ shiftStore.error }}
    </p>

    <div class="mt-1 border-t border-line pt-1">
      <RouterLink
        :to="{ name: 'crew', params: { id: crew.id } }"
        role="menuitem"
        class="block rounded-md px-2 py-1.5 text-accent no-underline hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        Открыть бригаду
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import UiDot from "@/components/ui/UiDot.vue";
import { formatTime } from "../model/format";
import { useBoardStore } from "../stores/board";
import { useCrewShiftStore } from "../stores/crewShift";

// Right-click menu of a crew row: take the crew off shift, and a link to the crew.
// Positioned where it was opened; closes on Escape or a click outside.

const MENU_WIDTH = 256;
const VIEWPORT_MARGIN = 8;

const board = useBoardStore();
const shiftStore = useCrewShiftStore();
const menuEl = ref<HTMLElement | null>(null);
const confirming = ref(false);

const crew = computed(() => {
  const crewId = shiftStore.menu?.crewId;
  return crewId == null ? null : (board.crews.find((row) => row.id === crewId) ?? null);
});

function confirm(): void {
  confirming.value = false;
  if (crew.value) void shiftStore.takeOffShift(crew.value.id);
}

// keep the menu inside the viewport; the height is measured once it is rendered
const menuHeight = ref(0);
const position = computed(() => {
  const x = shiftStore.menu?.x ?? 0;
  const y = shiftStore.menu?.y ?? 0;
  const maxLeft = window.innerWidth - MENU_WIDTH - VIEWPORT_MARGIN;
  const maxTop = window.innerHeight - menuHeight.value - VIEWPORT_MARGIN;
  return { left: Math.max(VIEWPORT_MARGIN, Math.min(x, maxLeft)), top: Math.max(VIEWPORT_MARGIN, Math.min(y, maxTop)) };
});

watch(
  () => shiftStore.menu,
  async (menu) => {
    confirming.value = false;
    if (!menu) return;
    await nextTick();
    menuHeight.value = menuEl.value?.offsetHeight ?? 0;
    menuEl.value?.querySelector<HTMLElement>("[role=menuitem]")?.focus();
  },
);

function onPointerDown(event: PointerEvent): void {
  if (shiftStore.menu && menuEl.value && !menuEl.value.contains(event.target as Node)) shiftStore.closeMenu();
}

function onKeyDown(event: KeyboardEvent): void {
  if (event.key === "Escape" && shiftStore.menu) shiftStore.closeMenu();
}

onMounted(() => {
  // capture: runs before the click (or right-click) that may open a menu elsewhere
  document.addEventListener("pointerdown", onPointerDown, true);
  document.addEventListener("keydown", onKeyDown);
});
onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onPointerDown, true);
  document.removeEventListener("keydown", onKeyDown);
  // the store outlives the board (e.g. after "Открыть бригаду"): don't reopen the menu on the next visit
  shiftStore.closeMenu();
});
</script>
