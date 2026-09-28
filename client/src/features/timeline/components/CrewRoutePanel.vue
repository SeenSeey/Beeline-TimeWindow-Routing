<template>
  <UiPanel>
    <template #title>
      <RouterLink
        v-if="crew"
        :to="{ name: 'crew', params: { id: crew.id } }"
        class="rounded-sm text-ink no-underline hover:text-accent hover:underline focus:outline-hidden focus-visible:ring-2 focus-visible:ring-accent/50"
      >
        {{ crew.name }}
      </RouterLink>
      <template v-else>Маршрут бригады</template>
    </template>
    <template v-if="crew" #aside>
      {{ regionName }} · смена {{ formatTime(crew.shiftStartTime) }}–{{ formatTime(crew.shiftEndTime) }}
      <template v-if="crew.vehicle"> · {{ crew.vehicle.displayName }}</template>
    </template>

    <UiEmptyState v-if="!crew">Выберите бригаду в расписании — здесь появится её маршрут на карте.</UiEmptyState>
    <UiEmptyState v-else-if="!crew.requests.length">У этой бригады нет заявок на выбранную дату.</UiEmptyState>

    <template v-else>
      <!-- the route's length: from the office through every visit, along the roads -->
      <div class="flex flex-wrap items-baseline gap-x-2 gap-y-1 border-b border-line px-5 py-3 text-sm">
        <span class="text-muted">Протяжённость маршрута</span>
        <b v-if="crew.routeDistanceKilometers != null" class="mono font-semibold text-ink">
          {{ formatDistance(crew.routeDistanceKilometers) }}
        </b>
        <span v-else class="text-faint">маршрут ещё не построен</span>
      </div>

      <!-- kept mounted while switching crews, so the map isn't re-created each time -->
      <div class="h-[840px] overflow-hidden rounded-b-lg">
        <CrewRouteMap :visits="crew.requests" :route="route" />
      </div>
    </template>
  </UiPanel>
</template>

<script setup lang="ts">
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatDistance, formatTime } from "../model/format";
import type { CrewRow } from "../model/timeline";
import type { CrewRoute } from "../types";
import CrewRouteMap from "./CrewRouteMap.vue";

defineProps<{
  crew: CrewRow | null;
  /** the crew's road route; null while loading or when there is none */
  route: CrewRoute | null;
  regionName: string;
}>();
</script>
