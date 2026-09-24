<template>
  <!-- shows the theme it switches to: a moon in light mode, a sun in dark mode -->
  <button
    type="button"
    class="inline-flex size-9 shrink-0 cursor-pointer items-center justify-center rounded-full text-muted hover:bg-shift hover:text-ink focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
    :aria-label="label"
    :title="label"
    @click="toggleTheme"
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
      <template v-if="activeTheme === 'dark'">
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
      </template>
      <path v-else d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
    </svg>
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useTheme } from "@/composables/useTheme";

const { activeTheme, toggleTheme } = useTheme();

const label = computed(() => (activeTheme.value === "dark" ? "Включить светлую тему" : "Включить тёмную тему"));
</script>
