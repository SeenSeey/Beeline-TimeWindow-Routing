<template>
  <!-- which version of the schedule is on the board, as one compact line: ‹ version › — each recalculation
       leaves the schedule before it behind. Nothing to show while there is only one version -->
  <div
    v-if="historical || schedule.previousScheduleId != null"
    class="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-xs"
    role="group"
    aria-label="Версии расписания"
  >
    <!-- the same neutral outline in both states: an earlier version is told by its tinted label -->
    <div class="inline-flex h-8 items-stretch overflow-hidden rounded-lg border border-line shadow-2xs">
      <button
        type="button"
        class="inline-flex w-8 cursor-pointer items-center justify-center bg-surface text-muted hover:bg-shift hover:text-ink focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-inset disabled:pointer-events-none disabled:opacity-40"
        :disabled="schedule.previousScheduleId == null || loading || disabled"
        :title="disabled ? 'Сначала завершите редактирование' : 'Предыдущая версия'"
        aria-label="Предыдущая версия"
        @click="emit('older')"
      >
        <svg
          class="size-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="m15 18-6-6 6-6" />
        </svg>
      </button>

      <span
        class="inline-flex items-center gap-x-1.5 border-x border-line px-2.5 whitespace-nowrap"
        :class="
          historical ? 'bg-[color-mix(in_srgb,var(--job-addon)_14%,var(--surface))] text-ink' : 'bg-surface text-muted'
        "
        aria-live="polite"
      >
        <UiDot v-if="loading" size="sm" color="var(--accent)" pulse />
        <svg
          v-else
          class="size-3.5"
          :class="{ 'text-[var(--job-addon)]': historical }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5M12 7v5l3 2" />
        </svg>
        <template v-if="historical">
          Версия от <span class="mono font-medium">{{ createdAt }}</span>
        </template>
        <template v-else>Текущая версия</template>
      </span>

      <button
        type="button"
        class="inline-flex w-8 cursor-pointer items-center justify-center bg-surface text-muted hover:bg-shift hover:text-ink focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 focus-visible:ring-inset disabled:pointer-events-none disabled:opacity-40"
        :disabled="!historical || loading"
        title="Более новая версия"
        aria-label="Более новая версия"
        @click="emit('newer')"
      >
        <svg
          class="size-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="m9 18 6-6-6-6" />
        </svg>
      </button>
    </div>

    <button
      v-if="historical && stepsBack > 1"
      type="button"
      class="cursor-pointer rounded-sm font-medium text-accent hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50 disabled:opacity-50"
      :disabled="loading"
      @click="emit('current')"
    >
      К текущей
    </button>

    <span v-if="error" class="text-danger" role="alert">{{ error }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiDot from "@/components/ui/UiDot.vue";
import { formatDateTime } from "../model/format";
import type { ScheduleData } from "../types";

const props = defineProps<{
  /** the version on the board */
  schedule: ScheduleData;
  /** it is an earlier version, not the current one */
  historical: boolean;
  /** how many versions back it is; 0 for the current one */
  stepsBack: number;
  /** an earlier version is loading */
  loading: boolean;
  /** why the last one failed to load */
  error: string | null;
  /** switching back is blocked (the current version is being edited) */
  disabled: boolean;
}>();

const emit = defineEmits<{
  older: [];
  newer: [];
  current: [];
}>();

const createdAt = computed(() => (props.schedule.createdAt ? formatDateTime(props.schedule.createdAt) : "—"));
</script>
