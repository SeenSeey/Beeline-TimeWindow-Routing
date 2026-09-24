<template>
  <section class="flex min-w-0 flex-col gap-4 rounded-lg border border-line bg-surface p-5 shadow-2xs">
    <div class="flex items-start gap-x-3">
      <span
        class="inline-flex size-9 shrink-0 items-center justify-center rounded-md bg-current/15"
        :style="{ color: danger ? 'var(--unassigned)' : 'var(--accent)' }"
        aria-hidden="true"
      >
        <slot name="icon" />
      </span>
      <div class="grid gap-1">
        <h2 class="m-0 text-base font-semibold text-ink">{{ title }}</h2>
        <p class="m-0 text-sm text-muted">{{ description }}</p>
      </div>
    </div>

    <!-- what the action does, as a short list -->
    <ul v-if="effects.length" class="m-0 grid list-disc gap-1 ps-5 text-sm text-muted marker:text-faint">
      <li v-for="effect in effects" :key="effect">{{ effect }}</li>
    </ul>

    <div class="mt-auto grid gap-3">
      <!-- a dangerous action asks again before it runs -->
      <div
        v-if="confirming"
        class="grid gap-3 rounded-md border border-danger/40 bg-danger/10 p-3"
        role="alertdialog"
        :aria-label="confirmText"
      >
        <p class="m-0 text-sm font-medium text-danger">{{ confirmText }}</p>
        <div class="flex flex-wrap gap-2">
          <button
            type="button"
            class="inline-flex h-9.5 cursor-pointer items-center justify-center rounded-lg bg-danger px-3 text-sm font-medium text-white hover:brightness-110 focus:outline-hidden focus-visible:ring-2 focus-visible:ring-danger/50"
            @click="confirm"
          >
            {{ confirmLabel }}
          </button>
          <UiButton @click="confirming = false">Отмена</UiButton>
        </div>
      </div>

      <div v-else class="flex flex-wrap items-center gap-3">
        <UiButton
          :variant="danger ? 'outline' : 'solid'"
          :class="{ 'text-danger!': danger }"
          :disabled="disabled || result.kind === 'running'"
          @click="start"
        >
          <UiDot v-if="result.kind === 'running'" size="sm" pulse />
          {{ result.kind === "running" ? runningLabel : actionLabel }}
        </UiButton>
      </div>

      <p
        v-if="result.kind === 'success' || result.kind === 'error'"
        class="m-0 rounded-md px-3 py-2 text-xs"
        :class="result.kind === 'success' ? 'bg-current/10 text-[var(--job-local)]' : 'bg-danger/10 text-danger'"
        :role="result.kind === 'error' ? 'alert' : 'status'"
      >
        {{ result.message }}
        <span class="mono opacity-70"> · {{ formatClock(result.finishedAt) }}</span>
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import UiButton from "@/components/ui/UiButton.vue";
import UiDot from "@/components/ui/UiDot.vue";
import type { SystemActionResult } from "../stores/systemActions";

const props = withDefaults(
  defineProps<{
    title: string;
    description: string;
    /** consequences, listed under the description */
    effects?: string[];
    actionLabel: string;
    runningLabel: string;
    result: SystemActionResult;
    /** another action is running */
    disabled?: boolean;
    /** destructive: red styling and a confirmation step */
    danger?: boolean;
    confirmText?: string;
    confirmLabel?: string;
  }>(),
  { effects: () => [], disabled: false, danger: false, confirmText: "Вы уверены?", confirmLabel: "Подтвердить" },
);

const emit = defineEmits<{
  run: [];
}>();

const confirming = ref(false);

function start(): void {
  if (props.danger) confirming.value = true;
  else emit("run");
}

function confirm(): void {
  confirming.value = false;
  emit("run");
}

const formatClock = (date: Date): string =>
  date.toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
</script>
