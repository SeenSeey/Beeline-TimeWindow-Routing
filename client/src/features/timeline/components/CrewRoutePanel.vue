<template>
  <UiPanel :title="crew ? crew.name : 'Маршрут бригады'">
    <template v-if="crew" #aside>
      {{ regionName }} · смена {{ formatTime(crew.shiftStartTime) }}–{{ formatTime(crew.shiftEndTime) }}
      <template v-if="crew.vehicle"> · {{ crew.vehicle.displayName }}</template>
    </template>

    <UiEmptyState v-if="!crew">Выберите бригаду в расписании — здесь появится её маршрут на карте.</UiEmptyState>
    <UiEmptyState v-else-if="!crew.requests.length">У этой бригады нет заявок на выбранную дату.</UiEmptyState>

    <!-- kept mounted while switching crews, so the map isn't re-created each time -->
    <div v-else class="h-[420px] overflow-hidden rounded-b-lg">
      <CrewRouteMap :visits="crew.requests" />
    </div>
  </UiPanel>
</template>

<script setup lang="ts">
import UiEmptyState from "@/components/ui/UiEmptyState.vue";
import UiPanel from "@/components/ui/UiPanel.vue";
import { formatTime } from "../model/format";
import type { CrewRow } from "../model/timeline";
import CrewRouteMap from "./CrewRouteMap.vue";

defineProps<{
  crew: CrewRow | null;
  regionName: string;
}>();
</script>
