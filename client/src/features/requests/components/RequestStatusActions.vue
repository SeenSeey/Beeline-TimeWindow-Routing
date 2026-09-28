<template>
  <div class="grid justify-items-end gap-2">
    <!-- canceling can't be undone here, so it asks once more -->
    <div
      v-if="confirmingCancel"
      class="flex flex-wrap items-center justify-end gap-2 rounded-lg border border-danger/40 bg-danger/10 p-2 ps-3"
      role="alertdialog"
      :aria-label="`Отменить заявку №${request.id}?`"
    >
      <span class="text-sm font-medium text-danger">Отменить заявку №{{ request.id }}?</span>
      <button
        type="button"
        class="inline-flex h-9.5 cursor-pointer items-center justify-center gap-x-2 rounded-lg bg-danger px-3 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-danger/50"
        @click="confirmCancel"
      >
        Отменить
      </button>
      <UiButton @click="confirmingCancel = false">Нет</UiButton>
    </div>

    <!-- only the steps the request's current status allows, as the server checks them -->
    <div v-else-if="canStart || canComplete || canCancel" class="flex flex-wrap justify-end gap-2">
      <UiButton v-if="canStart" :disabled="!!pendingAction" @click="emit('change', 'in_progress')">
        <UiDot v-if="pendingAction === 'in_progress'" size="sm" pulse />
        <svg
          v-else
          class="size-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <polygon points="6 3 20 12 6 21 6 3" />
        </svg>
        {{ pendingAction === "in_progress" ? "Начинаем…" : "Взять в работу" }}
      </UiButton>

      <button
        v-if="canComplete"
        type="button"
        class="inline-flex h-9.5 cursor-pointer items-center justify-center gap-x-2 rounded-lg border border-transparent bg-[var(--job-local)] px-3 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:pointer-events-none disabled:opacity-50"
        :disabled="!!pendingAction"
        @click="emit('change', 'complete')"
      >
        <UiDot v-if="pendingAction === 'complete'" size="sm" pulse />
        <svg
          v-else
          class="size-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
        {{ pendingAction === "complete" ? "Завершаем…" : "Завершить" }}
      </button>

      <UiButton v-if="canCancel" class="text-danger!" :disabled="!!pendingAction" @click="confirmingCancel = true">
        <UiDot v-if="pendingAction === 'cancel'" size="sm" pulse />
        <svg
          v-else
          class="size-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="10" />
          <path d="m15 9-6 6M9 9l6 6" />
        </svg>
        {{ pendingAction === "cancel" ? "Отменяем…" : "Отменить заявку" }}
      </UiButton>
    </div>

    <p v-if="error" class="m-0 max-w-sm rounded-md bg-danger/10 px-3 py-1.5 text-xs text-danger" role="alert">
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import type { RequestStatusAction } from "../api/requestsApi";
import { availableStatusActions } from "../model/statusActions";
import type { RequestDetailed } from "../types";

const props = defineProps<{
  request: RequestDetailed;
  /** the status change in flight */
  pendingAction: RequestStatusAction | null;
  /** why the last status change failed */
  error: string | null;
}>();

const emit = defineEmits<{
  change: [action: RequestStatusAction];
}>();

// the same rules the server applies, shared with the timeline's menu
const available = computed(() =>
  availableStatusActions({
    statusKeyword: props.request.status?.keyword ?? null,
    completedAt: props.request.completedAt,
    canceledAt: props.request.canceledAt,
  }),
);
const canStart = computed(() => available.value.in_progress);
const canComplete = computed(() => available.value.complete);
const canCancel = computed(() => available.value.cancel);

const confirmingCancel = ref(false);

function confirmCancel(): void {
  confirmingCancel.value = false;
  emit("change", "cancel");
}
</script>
