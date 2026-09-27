<template>
  <div class="grid justify-items-end gap-2">
    <!-- taking a crew off shift reshuffles today's schedule and can't be undone here, so it asks once more -->
    <div
      v-if="confirming"
      class="flex flex-wrap items-center justify-end gap-2 rounded-lg border border-danger/40 bg-danger/10 p-2 ps-3"
      role="alertdialog"
      :aria-label="`Снять бригаду «${crewName}» со смены?`"
    >
      <span class="text-sm font-medium text-danger">Снять «{{ crewName }}» со смены?</span>
      <button
        type="button"
        class="inline-flex h-9.5 cursor-pointer items-center justify-center gap-x-2 rounded-lg bg-danger px-3 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-danger/50"
        @click="confirm"
      >
        Да, снять
      </button>
      <UiButton @click="confirming = false">Нет</UiButton>
    </div>

    <p v-else-if="done" class="m-0 rounded-md bg-shift px-3 py-2 text-sm text-muted" role="status">
      Бригада снята со смены, её заявки перераспределены
    </p>

    <UiButton v-else class="text-danger!" :disabled="pending" @click="confirming = true">
      <UiDot v-if="pending" size="sm" pulse />
      <svg v-else class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <path d="m16 17 5-5-5-5M21 12H9" />
      </svg>
      {{ pending ? "Снимаем…" : "Снять со смены" }}
    </UiButton>

    <p v-if="error" class="m-0 max-w-sm rounded-md bg-danger/10 px-3 py-1.5 text-xs text-danger" role="alert">
      {{ error }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";

defineProps<{
  crewName: string;
  /** the request is in flight */
  pending: boolean;
  /** the crew was taken off shift from this page */
  done: boolean;
  /** why the last attempt failed */
  error: string | null;
}>();

const emit = defineEmits<{
  takeOffShift: [];
}>();

const confirming = ref(false);

function confirm(): void {
  confirming.value = false;
  emit("takeOffShift");
}
</script>
