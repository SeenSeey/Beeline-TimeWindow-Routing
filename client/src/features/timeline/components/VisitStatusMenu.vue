<template>
  <div
    v-if="statusStore.menu && visit"
    ref="menuEl"
    class="fixed z-40 grid w-64 gap-1 rounded-lg border border-line bg-surface p-2 text-sm shadow-lg"
    :style="{ left: position.left + 'px', top: position.top + 'px' }"
    role="menu"
    :aria-label="`Статус заявки #${visit.slot.requestId}`"
  >
    <div class="grid gap-0.5 px-2 pt-1 pb-2">
      <span class="mono font-semibold text-ink">Заявка #{{ visit.slot.requestId }}</span>
      <span class="truncate text-xs text-muted" :title="visit.slot.fullAddress">{{ visit.slot.fullAddress }}</span>
      <span class="text-xs text-muted">
        {{ visit.crewName }} · {{ visit.slot.status?.displayName ?? "без статуса" }}
      </span>
    </div>

    <!-- canceling can't be undone here, so it asks once more -->
    <div v-if="confirmingCancel" class="grid gap-2 rounded-md bg-danger/10 p-2" role="alertdialog">
      <span class="text-sm font-medium text-danger">Отменить заявку #{{ visit.slot.requestId }}?</span>
      <div class="flex gap-2">
        <button
          type="button"
          role="menuitem"
          class="inline-flex h-8 flex-1 cursor-pointer items-center justify-center rounded-md bg-danger px-2 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-danger/50"
          @click="run('cancel')"
        >
          Да, отменить
        </button>
        <button
          type="button"
          role="menuitem"
          class="inline-flex h-8 flex-1 cursor-pointer items-center justify-center rounded-md border border-line bg-surface px-2 text-sm text-ink hover:bg-shift focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
          @click="confirmingCancel = false"
        >
          Нет
        </button>
      </div>
    </div>

    <!-- only the steps the request's status allows -->
    <template v-else>
      <button
        v-for="action in actions"
        :key="action"
        type="button"
        role="menuitem"
        class="flex w-full cursor-pointer items-center gap-x-2 rounded-md px-2 py-1.5 text-start focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
        :class="ACTION_STYLES[action]"
        :disabled="!!statusStore.pendingAction"
        @click="action === 'cancel' ? (confirmingCancel = true) : run(action)"
      >
        <UiDot v-if="statusStore.pendingAction === action" size="sm" pulse />
        <UiDot v-else size="sm" :color="ACTION_DOTS[action]" />
        {{ statusStore.pendingAction === action ? STATUS_ACTION_LABELS[action].running : STATUS_ACTION_LABELS[action].idle }}
      </button>
      <span v-if="!actions.length" class="px-2 py-1.5 text-xs text-faint">Статус заявки больше не меняется</span>
    </template>

    <p v-if="statusStore.error" class="m-0 rounded-md bg-danger/10 px-2 py-1.5 text-xs text-danger" role="alert">
      {{ statusStore.error }}
    </p>

    <div class="mt-1 border-t border-line pt-1">
      <RouterLink
        :to="{ name: 'request', params: { id: visit.slot.requestId } }"
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
import UiDot from "@/components/ui/UiDot.vue";
import type { RequestStatusAction } from "@/features/requests/api/requestsApi";
import { availableStatusActions, STATUS_ACTION_LABELS } from "@/features/requests/model/statusActions";
import { useBoardStore } from "../stores/board";
import { useVisitStatusStore } from "../stores/visitStatus";

// Right-click menu of a work bar: the status changes the request allows, and a link to it.
// Positioned where it was opened; closes on Escape or a click outside.

const MENU_WIDTH = 256;
const VIEWPORT_MARGIN = 8;

const ACTION_ORDER: RequestStatusAction[] = ["in_progress", "complete", "cancel"];
const ACTION_STYLES: Record<RequestStatusAction, string> = {
  in_progress: "text-ink hover:bg-shift",
  complete: "text-ink hover:bg-shift",
  cancel: "text-danger hover:bg-danger/10",
};
const ACTION_DOTS: Record<RequestStatusAction, string> = {
  in_progress: "var(--accent)",
  complete: "var(--job-local)",
  cancel: "var(--unassigned)",
};

const board = useBoardStore();
const statusStore = useVisitStatusStore();
const menuEl = ref<HTMLElement | null>(null);
const confirmingCancel = ref(false);

const visit = computed(() => {
  const requestId = statusStore.menu?.requestId;
  return requestId == null ? null : (board.visits.find((v) => v.slot.requestId === requestId) ?? null);
});

const actions = computed<RequestStatusAction[]>(() => {
  if (!visit.value) return [];
  const { status, completedAt, canceledAt } = visit.value.slot;
  const available = availableStatusActions({ statusKeyword: status?.keyword ?? null, completedAt, canceledAt });
  return ACTION_ORDER.filter((action) => available[action]);
});

function run(action: RequestStatusAction): void {
  confirmingCancel.value = false;
  if (visit.value) void statusStore.changeStatus(visit.value.slot.requestId, action);
}

// keep the menu inside the viewport; the height is measured once it is rendered
const menuHeight = ref(0);
const position = computed(() => {
  const x = statusStore.menu?.x ?? 0;
  const y = statusStore.menu?.y ?? 0;
  const maxLeft = window.innerWidth - MENU_WIDTH - VIEWPORT_MARGIN;
  const maxTop = window.innerHeight - menuHeight.value - VIEWPORT_MARGIN;
  return { left: Math.max(VIEWPORT_MARGIN, Math.min(x, maxLeft)), top: Math.max(VIEWPORT_MARGIN, Math.min(y, maxTop)) };
});

watch(
  () => statusStore.menu,
  async (menu) => {
    confirmingCancel.value = false;
    if (!menu) return;
    await nextTick();
    menuHeight.value = menuEl.value?.offsetHeight ?? 0;
    menuEl.value?.querySelector<HTMLElement>("[role=menuitem]")?.focus();
  },
);

function onPointerDown(event: PointerEvent): void {
  if (statusStore.menu && menuEl.value && !menuEl.value.contains(event.target as Node)) statusStore.closeMenu();
}

function onKeyDown(event: KeyboardEvent): void {
  if (event.key === "Escape" && statusStore.menu) statusStore.closeMenu();
}

onMounted(() => {
  // capture: runs before the click (or right-click) that may open a menu elsewhere
  document.addEventListener("pointerdown", onPointerDown, true);
  document.addEventListener("keydown", onKeyDown);
});
onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onPointerDown, true);
  document.removeEventListener("keydown", onKeyDown);
});
</script>
