<template>
  <!-- same page frame as the request details -->
  <div class="grid grid-cols-[minmax(0,1fr)] content-start gap-4 p-4 sm:gap-6 sm:p-6">
    <!-- 1. who: name and number -->
    <header class="grid gap-2">
      <RouterLink
        :to="{ name: 'crews' }"
        class="inline-flex w-fit items-center gap-x-1.5 rounded-sm text-sm text-muted no-underline hover:text-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Все исполнители
      </RouterLink>

      <div class="flex flex-wrap items-start justify-between gap-x-6 gap-y-3">
        <div class="flex flex-wrap items-center gap-x-3 gap-y-2">
          <h1 class="m-0 text-2xl font-semibold text-balance text-ink">{{ crew.name }}</h1>
          <span class="mono text-sm text-muted">№{{ crew.id }}</span>
        </div>

        <CrewOffShiftAction
          :crew-name="crew.name"
          :pending="offShiftPending"
          :done="offShiftDone"
          :error="offShiftError"
          @take-off-shift="emit('takeOffShift')"
        />
      </div>
    </header>

    <!-- 2. like the request's page: the crew and its route on the left; its requests on the side -->
    <div class="grid grid-cols-1 items-start gap-4 sm:gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
      <div class="grid min-w-0 gap-4 sm:gap-6">
        <CrewInfoPanel :crew="crew" />
        <CrewRoutePanel
          :visits="crew.route"
          :road-route="crew.roadRoute ?? null"
          :route-date="crew.routeDate"
          :region-id="crew.region.id"
        />
      </div>

      <CrewRequestsPanel :requests="crew.assignedRequests" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CrewDetailed } from "../types";
import CrewInfoPanel from "./CrewInfoPanel.vue";
import CrewOffShiftAction from "./CrewOffShiftAction.vue";
import CrewRequestsPanel from "./CrewRequestsPanel.vue";
import CrewRoutePanel from "./CrewRoutePanel.vue";

withDefaults(
  defineProps<{
    crew: CrewDetailed;
    /** taking the crew off shift is in flight */
    offShiftPending?: boolean;
    /** the crew was taken off shift from this page */
    offShiftDone?: boolean;
    /** why the last off-shift attempt failed */
    offShiftError?: string | null;
  }>(),
  { offShiftPending: false, offShiftDone: false, offShiftError: null },
);

const emit = defineEmits<{
  takeOffShift: [];
}>();
</script>
