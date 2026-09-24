<template>
  <UiPanel :title="`Маршрут на ${formatDate(routeDate)}`">
    <template #aside>
      <!-- the schedule page shows today's schedule of a region -->
      <RouterLink
        :to="{ name: 'schedule', query: { region: regionId } }"
        class="rounded-sm font-medium text-accent no-underline hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        Открыть расписание
      </RouterLink>
    </template>

    <UiEmptyState v-if="!visits.length">В расписании на эту дату у бригады нет заявок.</UiEmptyState>

    <!-- the route on the map, like the one under the timeline -->
    <div v-if="visits.length" class="h-[360px] border-b border-line">
      <UiRouteMap :stops="stops" />
    </div>

    <!-- numbered like the pins on the map above -->
    <ol v-if="visits.length" class="m-0 grid list-none gap-2 p-3">
      <li
        v-for="(visit, index) in visits"
        :key="visit.requestId"
        class="grid grid-cols-[28px_1fr_auto] items-start gap-x-3 rounded-md bg-shift px-3 py-2.5"
      >
        <span
          class="mono inline-flex size-7 items-center justify-center rounded-full text-xs font-semibold text-white"
          :class="{ 'ring-2 ring-urgent ring-offset-1 ring-offset-shift': visit.urgent }"
          :style="{ background: jobTypeColor(visit.jobType.keyword) }"
        >
          {{ index + 1 }}
        </span>

        <div class="grid min-w-0 gap-1">
          <RouterLink
            :to="{ name: 'request', params: { id: visit.requestId } }"
            class="block truncate rounded-sm text-sm font-medium text-ink no-underline hover:text-accent focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
            :title="visit.fullAddress"
          >
            {{ visit.fullAddress }}
          </RouterLink>
          <div class="flex flex-wrap items-center gap-1.5 text-xs text-muted">
            <UiBadge :color="jobTypeColor(visit.jobType.keyword)">{{ visit.jobType.displayName }}</UiBadge>
            <UiBadge v-if="visit.status" :color="statusColor(visit.status.keyword)">{{ visit.status.displayName }}</UiBadge>
            <span class="mono">
              окно {{ formatTime(visit.requestTimeWindowStart) }}–{{ formatTime(visit.requestTimeWindowEnd) }}
            </span>
          </div>
        </div>

        <div class="mono text-end text-xs whitespace-nowrap text-ink">
          {{ formatTime(visit.startTime) }}–{{ formatTime(visit.endTime) }}
          <div class="text-muted">приезд {{ formatTime(visit.arrivalTime) }}</div>
          <div v-if="visit.finishedAt" class="text-[var(--job-local)]">завершено {{ formatTime(visit.finishedAt.slice(11)) }}</div>
        </div>
      </li>
    </ol>
  </UiPanel>
</template>

<script setup lang="ts">
import { computed } from "vue";
import UiBadge from "@/components/ui/UiBadge.vue";
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import UiRouteMap, { type UiRouteStop } from "@/components/ui/UiRouteMap.vue";
import { jobTypeColor, statusColor } from "@/features/requests/model/colors";
import { formatDate, formatTime } from "@/features/timeline/model/format";
import type { RouteVisit } from "../types";

const props = defineProps<{
  /** in visit order */
  visits: RouteVisit[];
  /** "YYYY-MM-DD" */
  routeDate: string;
  regionId: number;
}>();

// same numbers and colours as the list: job type fill, urgent outline
const stops = computed<UiRouteStop[]>(() =>
  props.visits.map((visit, index) => ({
    key: String(visit.requestId),
    longitude: visit.longitude,
    latitude: visit.latitude,
    label: String(index + 1),
    color: jobTypeColor(visit.jobType.keyword),
    outlineColor: visit.urgent ? "var(--urgent)" : undefined,
    to: { name: "request", params: { id: visit.requestId } },
    ariaLabel: `${index + 1}. ${formatTime(visit.startTime)}–${formatTime(visit.endTime)}, ${visit.fullAddress}`,
  })),
);
</script>
