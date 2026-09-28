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

    <!-- the route on the map, like the one under the timeline: along the roads from the office, start and
         finish marked; without a stored road route just the pins, no line -->
    <!-- the route's length: from the office through every visit, along the roads -->
    <div
      v-if="visits.length"
      class="flex flex-wrap items-baseline gap-x-2 gap-y-1 border-b border-line px-5 py-3 text-sm"
    >
      <span class="text-muted">Протяжённость маршрута</span>
      <b v-if="roadRoute" class="mono font-semibold text-ink">{{ formatDistance(roadRoute.distanceKilometers) }}</b>
      <span v-else class="text-faint">маршрут ещё не построен</span>
    </div>

    <div v-if="visits.length" class="h-[720px] border-b border-line">
      <UiRouteMap :stops="stops" :legs="legs" :leg-labels="legLabels" start-label="Старт" finish-label="Финиш" />
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
          <span class="mono text-xs text-muted" :title="`Координаты заявки #${visit.requestId}`">
            {{ formatCoordinates(visit.latitude, visit.longitude) }}
          </span>
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
import UiRouteMap, { type UiRouteStop, type UiRouteStopStatus } from "@/components/ui/UiRouteMap.vue";
import { jobTypeColor, statusColor } from "@/features/requests/model/colors";
import { routeLegLabels, routeLegs, type LngLat } from "@/features/timeline/model/crewRoute";
import { formatDate, formatDistance, formatTime } from "@/features/timeline/model/format";
import { formatCoordinates } from "../model/format";
import type { CrewRoute, RouteVisit } from "../types";

const props = defineProps<{
  /** in visit order */
  visits: RouteVisit[];
  /** the road route through the visits; null while there is none */
  roadRoute: CrewRoute | null;
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
    ariaLabel: `${index + 1}. ${formatTime(visit.startTime)}–${formatTime(visit.endTime)}, ${visit.fullAddress}${visit.status ? `, ${visit.status.displayName}` : ""}`,
    status: stopStatusOf(visit),
  })),
);

/** how far the visit got, as the timeline's bars tell it: by the request's status, or its visit finished */
function stopStatusOf(visit: RouteVisit): UiRouteStopStatus | undefined {
  const keyword = visit.status?.keyword;
  if (keyword === "CANCELED") return "canceled";
  if (keyword === "COMPLETED" || visit.finishedAt != null) return "done";
  if (keyword === "IN_PROGRESS") return "active";
  return undefined;
}

// no straight segments between the pins in place of a missing road route: they would pass for one
const legs = computed<LngLat[][]>(() => (props.roadRoute ? routeLegs(props.roadRoute) : []));
/** each leg's road distance, at its middle */
const legLabels = computed(() => (props.roadRoute ? routeLegLabels(props.roadRoute) : []));
</script>
