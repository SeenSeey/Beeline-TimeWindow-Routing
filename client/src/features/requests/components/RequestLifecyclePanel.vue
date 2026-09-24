<template>
  <UiPanel title="Ход выполнения">
    <!-- vertical stepper: done steps get a filled marker and their date, pending ones stay faint -->
    <ol class="m-0 grid list-none gap-0 px-5 py-4">
      <li v-for="(step, index) in steps" :key="step.label" class="relative grid grid-cols-[20px_1fr] gap-x-3 pb-5 last:pb-0">
        <!-- connector down to the next step -->
        <span
          v-if="index < steps.length - 1"
          class="absolute top-5 bottom-0 start-[9.5px] w-px"
          :class="steps[index + 1]?.at ? 'bg-accent' : 'bg-line-strong'"
          aria-hidden="true"
        ></span>

        <span
          class="relative z-[1] mt-0.5 inline-flex size-5 items-center justify-center rounded-full border-2"
          :class="step.at ? 'border-transparent' : 'border-line-strong bg-surface'"
          :style="step.at ? { background: step.color } : undefined"
          aria-hidden="true"
        >
          <svg
            v-if="step.at"
            class="size-3 text-white"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="3"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path v-if="step.canceled" d="M18 6 6 18M6 6l12 12" />
            <path v-else d="M20 6 9 17l-5-5" />
          </svg>
        </span>

        <div class="grid gap-0.5">
          <span class="text-sm font-medium" :class="step.at ? 'text-ink' : 'text-faint'">{{ step.label }}</span>
          <span class="mono text-xs" :class="step.at ? 'text-muted' : 'text-faint'">
            {{ step.at ? formatDateTime(step.at) : "—" }}
          </span>
        </div>
      </li>
    </ol>
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatDateTime } from "../model/format";
import type { RequestDetailed } from "../types";

const props = defineProps<{
  request: RequestDetailed;
}>();

interface LifecycleStep {
  label: string;
  /** ISO LocalDateTime; null while the step hasn't happened */
  at: string | null;
  color: string;
  canceled?: boolean;
}

/** Created → assigned → completed; a canceled request ends with the cancellation instead */
const steps = computed<LifecycleStep[]>(() => {
  const request = props.request;
  const finalStep: LifecycleStep = request.canceledAt
    ? { label: "Отменена", at: request.canceledAt, color: "var(--unassigned)", canceled: true }
    : { label: "Выполнена", at: request.completedAt, color: "var(--job-local)" };
  return [
    { label: "Создана", at: request.createdAt, color: "var(--accent)" },
    { label: "Назначена бригаде", at: request.assignedAt, color: "var(--accent)" },
    finalStep,
  ];
});
</script>
