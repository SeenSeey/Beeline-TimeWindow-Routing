<template>
  <label class="inline-flex items-center gap-x-2 text-sm text-muted">
    <span v-if="label" class="whitespace-nowrap">{{ label }}</span>
    <span class="relative inline-flex items-center">
      <!-- Preline select: white field, rounded-lg, blue focus ring -->
      <select
        v-model="selected"
        class="h-9.5 cursor-pointer appearance-none rounded-lg border border-line bg-surface py-2 ps-3 pe-9 text-sm text-ink shadow-2xs focus:border-accent focus:ring-2 focus:ring-accent/30 focus:outline-hidden disabled:pointer-events-none disabled:opacity-50"
        :disabled="disabled || !options.length"
        :aria-busy="busy"
      >
        <option v-if="selected == null" :value="null" disabled>{{ placeholder }}</option>
        <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
      <!-- chevron, or a pulsing dot while the new value is being applied -->
      <UiDot v-if="busy" class="pointer-events-none absolute end-3" size="sm" color="var(--accent)" pulse />
      <svg
        v-else
        class="pointer-events-none absolute end-3 size-3.5 text-muted"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="m7 15 5 5 5-5M7 9l5-5 5 5" />
      </svg>
    </span>
  </label>
</template>

<script setup lang="ts" generic="T extends string | number">
import UiDot from "./UiDot.vue";

withDefaults(
  defineProps<{
    options: { value: T; label: string }[];
    label?: string;
    placeholder?: string;
    busy?: boolean;
    disabled?: boolean;
  }>(),
  { label: undefined, placeholder: "—", busy: false, disabled: false },
);

const selected = defineModel<T | null>({ required: true });
</script>
