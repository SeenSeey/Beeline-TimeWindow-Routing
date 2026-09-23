<template>
  <label class="inline-flex items-center gap-2 text-muted">
    <span v-if="label">{{ label }}</span>
    <span class="relative inline-flex items-center">
      <select
        v-model="selected"
        class="cursor-pointer appearance-none rounded-full border border-line-strong bg-surface py-1 pr-8 pl-[11px] text-[13px] text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-default disabled:opacity-60"
        :disabled="disabled || !options.length"
        :aria-busy="busy"
      >
        <option v-if="selected == null" :value="null" disabled>{{ placeholder }}</option>
        <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
      <!-- chevron, or a pulsing dot while the new value is being applied -->
      <UiDot v-if="busy" class="pointer-events-none absolute right-3" size="sm" color="var(--accent)" pulse />
      <svg
        v-else
        class="pointer-events-none absolute right-3 h-2.5 w-2.5 text-muted"
        viewBox="0 0 10 10"
        aria-hidden="true"
      >
        <path d="M1.5 3.5 5 7l3.5-3.5" fill="none" stroke="currentColor" stroke-width="1.5" />
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
