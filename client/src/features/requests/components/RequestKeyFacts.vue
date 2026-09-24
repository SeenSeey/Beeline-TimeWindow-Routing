<template>
  <!-- the four things a dispatcher checks first, as tiles -->
  <dl class="m-0 grid gap-4 sm:grid-cols-2 sm:gap-6 xl:grid-cols-4">
    <div
      v-for="fact in facts"
      :key="fact.label"
      class="flex min-w-0 flex-col gap-y-1 rounded-lg border border-line bg-surface p-4 shadow-2xs"
    >
      <dt class="text-xs text-muted">{{ fact.label }}</dt>
      <dd
        class="m-0 truncate text-base font-semibold"
        :class="[fact.value == null ? 'text-faint' : 'text-ink', { mono: fact.mono }]"
        :title="fact.value ?? undefined"
      >
        {{ fact.value ?? fact.placeholder }}
      </dd>
      <dd v-if="fact.hint" class="m-0 truncate text-xs text-muted">{{ fact.hint }}</dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { formatDateTime, formatDuration, formatTime } from "../model/format";
import type { RequestDetailed } from "../types";

const props = defineProps<{
  request: RequestDetailed;
}>();

interface KeyFact {
  label: string;
  value: string | null;
  /** shown in place of a missing value */
  placeholder?: string;
  /** second, smaller line */
  hint?: string;
  mono?: boolean;
}

const facts = computed<KeyFact[]>(() => {
  const request = props.request;
  return [
    {
      label: "Окно визита",
      value: `${formatTime(request.requestTimeWindowStart)}–${formatTime(request.requestTimeWindowEnd)}`,
      mono: true,
    },
    { label: "Крайний срок", value: formatDateTime(request.deadline), mono: true },
    {
      label: "Тип работ",
      value: request.jobType.displayName,
      hint: `всего ${formatDuration(request.jobType.totalTimeSpanMinutes)}`,
    },
    {
      label: "Бригада",
      value: request.assignedCrew?.name ?? null,
      placeholder: "не назначена",
      hint: request.assignedCrew?.vehicle?.displayName,
    },
  ];
});
</script>
