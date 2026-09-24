<template>
  <label class="inline-flex items-center gap-x-2 text-sm text-muted">
    <span v-if="label" class="whitespace-nowrap">{{ label }}</span>
    <span class="relative inline-flex items-center">
      <svg
        v-if="search"
        class="pointer-events-none absolute start-3 size-3.5 text-muted"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
      </svg>
      <!-- same field look as UiSelect -->
      <input
        v-model="value"
        :type="search ? 'search' : 'text'"
        :placeholder="placeholder"
        class="h-9.5 w-full min-w-0 rounded-lg border border-line bg-surface py-2 pe-3 text-sm text-ink shadow-2xs placeholder:text-faint focus:border-accent focus:ring-2 focus:ring-accent/30 focus:outline-hidden"
        :class="search ? 'ps-8.5' : 'ps-3'"
      />
    </span>
  </label>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string;
    placeholder?: string;
    /** search field: magnifier icon and the browser's clear button */
    search?: boolean;
  }>(),
  { label: undefined, placeholder: undefined, search: false },
);

const value = defineModel<string>({ required: true });
</script>
