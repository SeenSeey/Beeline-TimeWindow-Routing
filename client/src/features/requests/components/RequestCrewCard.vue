<template>
  <!-- the assigned crew in a small card of its own: who, and a way to it; its details live on its page -->
  <section
    class="flex items-center gap-x-3 rounded-lg border border-line bg-surface px-4 py-3 shadow-2xs"
    aria-labelledby="request-crew-card"
  >
    <span
      class="inline-flex size-9 shrink-0 items-center justify-center rounded-full"
      :class="crew ? 'bg-accent-soft text-accent' : 'bg-shift text-faint'"
      aria-hidden="true"
    >
      <svg class="size-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
      </svg>
    </span>

    <div class="grid min-w-0 flex-1 gap-0.5">
      <h2 id="request-crew-card" class="m-0 text-xs font-normal text-muted">Бригада</h2>
      <span v-if="crew" class="truncate text-sm font-semibold text-ink" :title="crew.name">
        {{ crew.name }} <span class="mono font-normal text-muted">№{{ crew.id }}</span>
      </span>
      <span v-else class="text-sm text-faint">не назначена</span>
    </div>

    <RouterLink
      v-if="crew"
      :to="{ name: 'crew', params: { id: crew.id } }"
      class="inline-flex shrink-0 items-center gap-x-1 rounded-sm text-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      :aria-label="`Открыть бригаду ${crew.name}`"
    >
      Открыть
      <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m9 18 6-6-6-6" />
      </svg>
    </RouterLink>
  </section>
</template>

<script setup lang="ts">
import type { CrewDetails } from "../types";

defineProps<{
  /** null while no crew is assigned */
  crew: CrewDetails | null;
}>();
</script>
