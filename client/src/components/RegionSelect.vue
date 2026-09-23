<template>
  <label class="inline-flex items-center gap-2 text-(--muted)">
    <span>Регион</span>
    <span class="relative inline-flex items-center">
      <select
        v-model="regionId"
        class="cursor-pointer appearance-none rounded-full border border-(--line-strong) bg-(--surface) py-1 pr-8 pl-[11px] text-[13px] text-(--ink) focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-(--accent) disabled:cursor-default disabled:opacity-60"
        :disabled="!regions.length"
        :aria-busy="busy"
      >
        <option v-if="regionId == null" :value="null" disabled>—</option>
        <option v-for="r in regions" :key="r.id" :value="r.id">{{ r.displayName }}</option>
      </select>
      <!-- chevron, or a pulsing dot while the chosen region's schedule loads -->
      <span
        v-if="busy"
        class="pointer-events-none absolute right-3 h-[7px] w-[7px] rounded-full bg-(--accent) motion-safe:animate-pulse"
      ></span>
      <svg
        v-else
        class="pointer-events-none absolute right-3 h-2.5 w-2.5 text-(--muted)"
        viewBox="0 0 10 10"
        aria-hidden="true"
      >
        <path d="M1.5 3.5 5 7l3.5-3.5" fill="none" stroke="currentColor" stroke-width="1.5" />
      </svg>
    </span>
  </label>
</template>

<script setup lang="ts">
import type { RegionInfo } from "@/types.ts";

defineProps<{
  regions: RegionInfo[];
  busy?: boolean;
}>();

const regionId = defineModel<number | null>({ required: true });
</script>
